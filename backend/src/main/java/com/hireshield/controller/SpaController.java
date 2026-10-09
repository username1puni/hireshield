package com.hireshield.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The React app is served from /static. These are its client-side routes, so a page
 * refresh on /history should return index.html instead of a 404.
 */
@Controller
public class SpaController {

    @GetMapping({"/login", "/register", "/search", "/model", "/history", "/history/{id}", "/report", "/admin"})
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
