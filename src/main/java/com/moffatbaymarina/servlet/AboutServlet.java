package com.moffatbaymarina.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/*
 * Module 7 - About Us Page Backend
 * Developer: Jordan Dardar
 *
 * Provides a dedicated public endpoint for the About Us page.
 * The About Us page does not require authentication, so any visitor
 * can access the marina information through the /about route.
 */
@WebServlet("/about")
public class AboutServlet extends HttpServlet {

    /*
     * Handles GET requests sent to /about.
     * The request is forwarded to the existing About Us HTML page
     * so the browser receives the normal Moffat Bay Marina page.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher("/about.html").forward(request, response);
    }
}
