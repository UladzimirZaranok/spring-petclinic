package org.springframework.samples.petclinic.system;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.samples.petclinic.owner.OwnerNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(OwnerNotFoundException.class)
    public String handleOwnerNotFound(OwnerNotFoundException ex, org.springframework.ui.Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error"; // Assuming you have a generic error view named "error.html"
    }
}
