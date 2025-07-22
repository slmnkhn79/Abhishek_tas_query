package com.tas.poc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the main chat interface.
 * 
 * This controller handles the web UI routes and serves
 * the Thymeleaf templates for the chat interface.
 * 
 * @author TAS Query POC Team
 */
@Controller
public class HomeController {
    
    /**
     * Serves the main chat interface.
     * 
     * @return The name of the Thymeleaf template to render
     */
    @GetMapping("/")
    public String home() {
        return "chat";
    }
    
    /**
     * Serves the about page with system information.
     * 
     * @return The name of the about template
     */
    @GetMapping("/about")
    public String about() {
        return "about";
    }
}