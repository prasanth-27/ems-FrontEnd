package com.prasanth.training.ems.bmsfrontend.controller;

import com.prasanth.training.ems.bmsfrontend.models.BusInventory;
import com.prasanth.training.ems.bmsfrontend.service.BusSearchService;
import com.prasanth.training.ems.bmsfrontend.service.TicketBookingService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    private BusSearchService busSearchService;
    @Autowired
    private TicketBookingService ticketBookingService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        logger.info("Home page");

        String username = (String) session.getAttribute("username");
        String token = (String) session.getAttribute("Authorization-token");
        model.addAttribute("username", username);
        model.addAttribute("token", token);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        logger.info("Login page");
        return "login";
    }

    @PostMapping("/login")
    public String authenticateUser(@RequestParam String username,
                                   @RequestParam String password,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        logger.info("User attempting to log in with username: {}", username);
        RestTemplate restTemplate = new RestTemplate();
        String authServiceUrl = "http://localhost:9099/api/v1/auth/login?email=%s&password=%s".formatted(username,password); // Update with actual URL

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(authServiceUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // Authentication successful, redirect to home or dashboard
                session.setAttribute("username", username);
                session.setAttribute("Authorization-token", response.getHeaders().get("Authorization").get(0));
                session.setMaxInactiveInterval(5 * 60); // Set session timeout to 5 minutes
                logger.info("User {} authenticated successfully", username);
                return "redirect:/";
            } else {
                logger.warn("Authentication failed for user: {}", username);
                // Authentication failed
                redirectAttributes.addAttribute("error", "true");
                return "redirect:/login";
            }
        } catch (Exception e) {
            logger.error("Error during authentication for user {}: {}", username, e.getMessage());
            // Error communicating with auth service
            redirectAttributes.addAttribute("error", "true");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        logger.info("User logging out");
        session.invalidate(); // Invalidate the session
        redirectAttributes.addAttribute("message", "You have been logged out successfully.");
        return "redirect:/login"; // Redirect to login page
    }

    @GetMapping("/signup")
    public String signup() {
        logger.info("User signup page");
        return "signup"; // Create this Thymeleaf template for signup
    }

    @PostMapping("/signup")
    public String registerUser(@RequestParam String name,
                               @RequestParam String password,
                               @RequestParam String email,
                                 @RequestParam String phoneNumber,
                               RedirectAttributes redirectAttributes) {
        logger.info("User attempting to register with username: {}", email);
        RestTemplate restTemplate = new RestTemplate();
        String authServiceUrl = "http://localhost:9099/api/v1/auth/register"; // Update with actual URL

        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("name", name);
        userDetails.put("password", password);
        userDetails.put("email", email);
        userDetails.put("phoneNumber", phoneNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(userDetails, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(authServiceUrl, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // Registration successful
                redirectAttributes.addAttribute("message", "Registration successful. Please log in.");
                return "redirect:/login";
            } else {
                if(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
                    logger.warn("Registration failed for user: {} with status code: {}", email, response.getStatusCode());
                    redirectAttributes.addAttribute("error", "User already exists with email. Please try a different email.");
                    return "redirect:/signup";
                } else {
                    logger.error("Server error during registration for user: {} with status code: {}", email, response.getStatusCode());
                    redirectAttributes.addAttribute("error", "Server error during registration. Please try again later.");
                    return "redirect:/signup"; // Redirect to signup with error
                }

            }
        } catch (Exception e) {
            logger.error("Error during registration for user {}: {}", email, e.getMessage());
            redirectAttributes.addAttribute("error", "Unable to register user. Please try again.");
            return "redirect:/signup";
        }
    }

    @PostMapping("/searchbus")
    public String searchBus(@RequestParam("from") String from,
                            @RequestParam("to") String to,
                            Model model,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        logger.info("Searching for buses from {} to {}", from, to);

        String username = (String) session.getAttribute("username");
        if(username==null){
            return "redirect:/login"; // Redirect to login if user is not authenticated
        }

        if( from == null || to == null || from.isEmpty() || to.isEmpty()) {
            model.addAttribute("error", "Please provide valid 'from' and 'to' locations.");
            return "index";
        }

        try {
            List<BusInventory> result= busSearchService.searchbus(from, to, session);

            if (result.size()>0) {
                model.addAttribute("buses", result);
                session.setAttribute("buses", result);
                logger.info("Buses searched successfully {}",result);
                return "searchresults"; // Create this Thymeleaf template
            } else {
                logger.warn("No buses found for from: {}, to: {}", from, to);
                redirectAttributes.addAttribute("error", "No buses found.");
                return "redirect:/";
            }
        } catch (Exception e) {
            logger.error("Error searching for buses from {} to {}: {}", from, to, e.getMessage());
            redirectAttributes.addAttribute("searchError", "Error searching for buses.");
            return "redirect:/";
        }
    }

    @GetMapping("/book")
    public String bookBus(@RequestParam("busNumber") long busNumber, Model model) {
        // Fetch bus details if needed and add to model
        model.addAttribute("busNumber", busNumber);
        return "bookbus"; // Create this Thymeleaf template for the booking form
    }

    @PostMapping("/book")
    public String confirmBooking(@RequestParam("busNumber") long busNumber,
                                 @RequestParam("passengerNames") List<String> passengerNames,
                                 @RequestParam("passengerAges") List<Short> passengerAges,
                                 @RequestParam("passengerGenders") List<Character> passengerGenders,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        logger.info("Booking bus number {} ", busNumber);

        // Here you would typically call a service to handle the booking logic
        // For now, we will just simulate a successful booking

        // Add booking confirmation to the model or session
        logger.info("PassengerNames {} ", passengerNames);
        logger.info("PassengerAges {} ", passengerAges);
        logger.info("PassengerGenders {} ", passengerGenders);
        if(!ticketBookingService.bookTicket(passengerNames, passengerAges, passengerGenders, busNumber, session, model))
        {
            logger.error("Failed to book ticket for bus number {}", busNumber);
            redirectAttributes.addAttribute("error", "Failed to book ticket. Please try again.");
            return "redirect:/book?busNumber=" + busNumber;
        }
        logger.info("Ticket booked successfully for bus number {}", busNumber);
        return "redirect:/"; // Create this Thymeleaf template for booking confirmation
    }

    @GetMapping("/history")
    public String bookingHistory(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        logger.info("Fetching booking history");

        String username = (String) session.getAttribute("username");
        String token = (String) session.getAttribute("Authorization-token");

        if (username == null || token == null) {
            logger.warn("User not logged in, redirecting to login page");
            redirectAttributes.addAttribute("error", "Please log in to view your booking history.");
            return "redirect:/login";
        }

        try {
            ResponseEntity<?> response = ticketBookingService.getBookingHistory(username, token);
            if (response.getStatusCode().is2xxSuccessful()) {
                List<?> tickets = (List<?>) response.getBody();
                model.addAttribute("tickets", tickets);
                return "bookinghistory"; // Create this Thymeleaf template
            } else {
                logger.error("Failed to fetch booking history: {}", response.getStatusCode());
                redirectAttributes.addAttribute("error", "Failed to fetch booking history.");
                return "redirect:/";
            }
        } catch (Exception e) {
            logger.error("Error fetching booking history: {}", e.getMessage());
            redirectAttributes.addAttribute("error", "Error fetching booking history.");
            return "redirect:/";
        }
    }
}
