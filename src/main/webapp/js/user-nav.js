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

            if (!data || !data.ok) {
                return null;
            }

            return data;
        } catch (error) {
            console.error("Unable to check login status:", error);
            return null;
        }
    }


    async function logout() {
        try {
            await fetch(API_BASE + "/logout", {
                method: "POST",
                credentials: "include",
                cache: "no-store",
                headers: {
                    "Accept": "application/json"
                }
            });
        } catch (error) {
            console.error("Unable to contact logout service:", error);
        }

        sessionStorage.removeItem("moffatLoggedInFirstName");
        sessionStorage.removeItem("moffatLoggedInEmail");

        localStorage.removeItem("moffatBayReservations");
        localStorage.removeItem("moffatBayCurrentReservationId");

        window.location.href = "index.html";
    }


    function createNavItem(text, href, id) {
        const li = document.createElement("li");
        const link = document.createElement("a");

        link.textContent = text;
        link.href = href;

        if (id) {
            link.id = id;
        }

        li.appendChild(link);

        return li;
    }


    function updateNavigation(user) {
        const nav = document.querySelector(
            'nav[aria-label="Main navigation"]'
        );

        if (!nav) {
            return;
        }

        const list = nav.querySelector("ul");

        /*
         * Some older pages may not use a <ul>.
         * Leave those pages unchanged rather than breaking them.
         */
        if (!list) {
            return;
        }

        const links = Array.from(
            list.querySelectorAll("a")
        );

        if (!user) {
            /*
             * Signed-out visitors keep the existing public navbar.
             */
            return;
        }


        /*
         * Store display-only account information for pages
         * that want to show the customer's name/email.
         * The server-side session is still the source of truth.
         */
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


        /*
         * Hide LOGIN and REGISTER whenever the server confirms
         * that the customer is already authenticated.
         */
        links.forEach(function (link) {
            const text =
                link.textContent.trim().toUpperCase();

            if (text === "LOGIN" || text === "REGISTER") {
                const item = link.closest("li");

                if (item) {
                    item.remove();
                } else {
                    link.remove();
                }
            }
        });


        /*
         * Add MY ACCOUNT if it is not already present.
         */
        const hasAccount = Array.from(
            list.querySelectorAll("a")
        ).some(function (link) {
            return (
                link.textContent.trim().toUpperCase() ===
                "MY ACCOUNT"
            );
        });

        if (!hasAccount) {
            list.appendChild(
                createNavItem(
                    "MY ACCOUNT",
                    "post_login.html"
                )
            );
        }


        /*
         * Add LOGOUT if it is not already present.
         */
        let logoutLink =
            document.getElementById("logoutLink");

        if (!logoutLink) {
            const logoutItem = createNavItem(
                "LOGOUT",
                "#",
                "logoutLink"
            );

            list.appendChild(logoutItem);

            logoutLink =
                logoutItem.querySelector("a");
        }


        /*
         * Only the LOGOUT link ends the login session.
         * HOME and all other links simply navigate normally.
         */
        logoutLink.addEventListener(
            "click",
            function (event) {
                event.preventDefault();
                logout();
            },
            { once: true }
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
