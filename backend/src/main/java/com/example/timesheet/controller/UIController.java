package com.example.timesheet.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to forward non-API requests to the frontend index.html.
 * This allows the React app to handle client-side routing in production.
 */
@Controller
public class UIController {

    /**
     * Forward common SPA routes to index.html so the client router can render the view.
     */
    @RequestMapping(value = {"/", "/login", "/dashboard/**", "/admin/**"})
    public String forward() {
        return "forward:/index.html";
    }
}


