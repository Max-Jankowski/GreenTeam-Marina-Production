/*
Alexander Baldree
Moffat Bay Marina
Shared authenticated navigation
*/

(function () {
    "use strict";

    const API_BASE =
        "https://obsidianserver.tail969ab5.ts.net/MoffatBayMarina";

    async function getCurrentUser() {
        try {
            const response = await fetch(API_BASE + "/current-user", {
                method: "GET",
                credentials: "include",
                cache: "no-store",
                headers: {
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                return null;
            }

            const data = await response.json();
            return data && data.ok ? data : null;
        } catch (error) {
            console.error("Unable to check login status:", error);
            return null;
        }
    }

    async function logout() {
        try {
            const response = await fetch(API_BASE + "/logout", {
                method: "POST",
                credentials: "include",
                cache: "no-store",
                headers: {
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {
                console.error("Logout request failed:", response.status);
            }
        } catch (error) {
            console.error("Unable to contact logout service:", error);
        }

        sessionStorage.removeItem("moffatLoggedInFirstName");
        sessionStorage.removeItem("moffatLoggedInEmail");

        localStorage.removeItem("moffatBayReservations");
        localStorage.removeItem("moffatBayCurrentReservationId");

        window.location.href = "index.html";
    }

    function createLink(text, href, id) {
        const link = document.createElement("a");
        link.textContent = text;
        link.href = href;

        if (id) {
            link.id = id;
        }

        return link;
    }

    function appendLink(nav, list, text, href, id) {
        const link = createLink(text, href, id);

        if (list) {
            const li = document.createElement("li");
            li.appendChild(link);
            list.appendChild(li);
        } else {
            nav.appendChild(link);
        }

        return link;
    }

    function hasLink(nav, text) {
        return Array.from(nav.querySelectorAll("a")).some(function (link) {
            return link.textContent.trim().toUpperCase() === text;
        });
    }

    function removeSignedOutLinks(nav) {
        Array.from(nav.querySelectorAll("a")).forEach(function (link) {
            const text = link.textContent.trim().toUpperCase();

            if (text === "LOGIN" || text === "REGISTER") {
                const li = link.closest("li");

                if (li) {
                    li.remove();
                } else {
                    link.remove();
                }
            }
        });
    }

    function updateNavigation(user) {
        const nav = document.querySelector(
            'nav[aria-label="Main navigation"]'
        );

        if (!nav || !user) {
            return;
        }

        if (user.firstName) {
            sessionStorage.setItem(
                "moffatLoggedInFirstName",
                user.firstName
            );
        }

        if (user.email) {
            sessionStorage.setItem(
                "moffatLoggedInEmail",
                user.email
            );
        }

        removeSignedOutLinks(nav);

        const list = nav.querySelector("ul");

        if (!hasLink(nav, "MY ACCOUNT")) {
            appendLink(
                nav,
                list,
                "MY ACCOUNT",
                "post_login.html"
            );
        }

        let logoutLink =
            document.getElementById("logoutLink");

        if (!logoutLink) {
            logoutLink = appendLink(
                nav,
                list,
                "LOGOUT",
                "#",
                "logoutLink"
            );
        } else {
            logoutLink.href = "#";
        }

        if (logoutLink.dataset.logoutBound === "true") {
            return;
        }

        logoutLink.dataset.logoutBound = "true";

        logoutLink.addEventListener(
            "click",
            function (event) {
                event.preventDefault();
                logout();
            }
        );
    }

    document.addEventListener(
        "DOMContentLoaded",
        async function () {
            const user = await getCurrentUser();
            updateNavigation(user);
        }
    );
})();
