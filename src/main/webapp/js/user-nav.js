/*
Alexander Baldree
Moffat Bay Marina
Shared login/logout navigation
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

            return data && data.ok
                ? data
                : null;

        } catch (error) {
            console.error(
                "Unable to check login status:",
                error
            );

            return null;
        }
    }


    async function logout() {

        const logoutLink =
            document.getElementById("logoutLink");

        if (logoutLink) {
            logoutLink.textContent =
                "LOGGING OUT...";

            logoutLink.style.pointerEvents =
                "none";
        }

        try {

            const response = await fetch(
                API_BASE + "/logout",
                {
                    method: "POST",
                    credentials: "include",
                    cache: "no-store",
                    headers: {
                        "Accept":
                            "application/json"
                    }
                }
            );


            if (!response.ok) {
                throw new Error(
                    "Logout endpoint returned " +
                    response.status
                );
            }


            const data =
                await response.json();


            if (!data || !data.ok) {
                throw new Error(
                    data && data.message
                        ? data.message
                        : "Logout was not confirmed."
                );
            }


            /*
             * Only clear browser-side account information
             * AFTER the server confirms that the HttpSession
             * was invalidated.
             */
            sessionStorage.removeItem(
                "moffatLoggedInFirstName"
            );

            sessionStorage.removeItem(
                "moffatLoggedInEmail"
            );

            localStorage.removeItem(
                "moffatBayReservations"
            );

            localStorage.removeItem(
                "moffatBayCurrentReservationId"
            );


            /*
             * Go home after successful server logout.
             */
            window.location.href =
                data.redirect ||
                "index.html";


        } catch (error) {

            console.error(
                "Logout failed:",
                error
            );


            /*
             * Stay on the current page if the backend
             * did not actually invalidate the session.
             */
            alert(
                "Logout could not be completed. " +
                "The server logout endpoint may not be deployed yet."
            );


            if (logoutLink) {
                logoutLink.textContent =
                    "LOGOUT";

                logoutLink.style.pointerEvents =
                    "";
            }
        }
    }


    function createLink(
        text,
        href,
        id
    ) {

        const link =
            document.createElement("a");

        link.textContent =
            text;

        link.href =
            href;


        if (id) {
            link.id =
                id;
        }


        return link;
    }


    function appendLink(
        nav,
        list,
        text,
        href,
        id
    ) {

        const link =
            createLink(
                text,
                href,
                id
            );


        if (list) {

            const item =
                document.createElement("li");

            item.appendChild(link);

            list.appendChild(item);

        } else {

            nav.appendChild(link);

        }


        return link;
    }


    function hasLink(
        nav,
        textToFind
    ) {

        return Array
            .from(
                nav.querySelectorAll("a")
            )
            .some(
                function (link) {

                    return (
                        link.textContent
                            .trim()
                            .toUpperCase()
                        ===
                        textToFind
                    );
                }
            );
    }


    function removeLinkByText(
        nav,
        textToRemove
    ) {

        Array
            .from(
                nav.querySelectorAll("a")
            )
            .forEach(
                function (link) {

                    const text =
                        link.textContent
                            .trim()
                            .toUpperCase();


                    if (
                        text ===
                        textToRemove
                    ) {

                        const item =
                            link.closest("li");


                        if (item) {
                            item.remove();
                        }
                        else {
                            link.remove();
                        }
                    }
                }
            );
    }


    function configureLoggedOutNav(
        nav,
        list
    ) {

        removeLinkByText(
            nav,
            "MY ACCOUNT"
        );

        removeLinkByText(
            nav,
            "LOGOUT"
        );


        /*
         * All signed-out pages should have LOGIN.
         */
        if (
            !hasLink(
                nav,
                "LOGIN"
            )
        ) {

            appendLink(
                nav,
                list,
                "LOGIN",
                "login.html"
            );
        }
    }


    function configureLoggedInNav(
        nav,
        list,
        user
    ) {

        removeLinkByText(
            nav,
            "LOGIN"
        );

        removeLinkByText(
            nav,
            "REGISTER"
        );


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


        if (
            !hasLink(
                nav,
                "MY ACCOUNT"
            )
        ) {

            appendLink(
                nav,
                list,
                "MY ACCOUNT",
                "post_login.html"
            );
        }


        let logoutLink =
            document.getElementById(
                "logoutLink"
            );


        if (!logoutLink) {

            logoutLink =
                appendLink(
                    nav,
                    list,
                    "LOGOUT",
                    "#",
                    "logoutLink"
                );
        }
        else {

            /*
             * Do NOT let the anchor navigate directly.
             * The server logout must finish first.
             */
            logoutLink.href =
                "#";
        }


        if (
            logoutLink.dataset
                .logoutBound ===
            "true"
        ) {

            return;
        }


        logoutLink.dataset.logoutBound =
            "true";


        logoutLink.addEventListener(
            "click",
            function (event) {

                event.preventDefault();

                logout();
            }
        );
    }


    async function initializeNavigation() {

        const nav =
            document.querySelector(
                'nav[aria-label="Main navigation"]'
            );


        if (!nav) {
            return;
        }


        const list =
            nav.querySelector("ul");


        const user =
            await getCurrentUser();


        if (user) {

            configureLoggedInNav(
                nav,
                list,
                user
            );
        }
        else {

            configureLoggedOutNav(
                nav,
                list
            );
        }
    }


    document.addEventListener(
        "DOMContentLoaded",
        initializeNavigation
    );
})();
