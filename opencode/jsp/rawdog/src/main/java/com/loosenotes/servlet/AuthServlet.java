package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.LoggerUtil;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.SessionUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;

public class AuthServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        
        if (action == null) {
            HttpSession session = request.getSession(false);
            if (SessionUtil.isLoggedIn(session)) {
                response.sendRedirect(request.getContextPath() + "/notes");
            } else {
                request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            }
        } else if ("register".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        } else if ("resetRequest".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
        } else if ("resetToken".equals(action)) {
            handleResetTokenView(request, response);
        } else if ("logout".equals(action)) {
            handleLogout(request, response);
        } else {
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        
        if (action == null) {
            handleLogin(request, response);
        } else if ("register".equals(action)) {
            handleRegister(request, response);
        } else if ("resetRequest".equals(action)) {
            handleResetRequest(request, response);
        } else if ("resetToken".equals(action)) {
            handleResetToken(request, response);
        } else {
            handleLogin(request, response);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(password)) {
            request.setAttribute("error", "Username and password are required");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
            return;
        }

        userDAO.findByUsername(username).ifPresentOrElse(
            user -> {
                if (user.isActive() && PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                    HttpSession session = request.getSession();
                    SessionUtil.setUser(session, user);
                    LoggerUtil.logLogin(user.getId(), request);
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    LoggerUtil.logLoginFailed(username, request);
                    try {
                        request.setAttribute("error", "Invalid username or password");
                        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                LoggerUtil.logLoginFailed(username, request);
                try {
                    request.setAttribute("error", "Invalid username or password");
                    request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(email) ||
            !ValidationUtil.isNotEmpty(password) || !ValidationUtil.isNotEmpty(confirmPassword)) {
            request.setAttribute("error", "All fields are required");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (!ValidationUtil.isValidUsername(username)) {
            request.setAttribute("error", "Username must be 3-50 characters and contain only letters, numbers, and underscores");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            request.setAttribute("error", "Please enter a valid email address");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            request.setAttribute("error", "Password must be at least 8 characters");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (userDAO.usernameExists(username)) {
            request.setAttribute("error", "Username already exists");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (userDAO.emailExists(email)) {
            request.setAttribute("error", "Email already exists");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        User user = new User(username, email, PasswordUtil.hashPassword(password));
        if (userDAO.createUser(user)) {
            LoggerUtil.logRegister(user.getId(), username, request);
            request.setAttribute("success", "Registration successful! Please log in.");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        } else {
            request.setAttribute("error", "Registration failed. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }

    private void handleResetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");

        if (!ValidationUtil.isNotEmpty(email)) {
            request.setAttribute("error", "Email is required");
            request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
            return;
        }

        userDAO.findByEmail(email).ifPresentOrElse(
            user -> {
                String resetToken = PasswordUtil.generateResetToken();
                LocalDateTime expiry = LocalDateTime.now().plusHours(1);
                userDAO.setResetToken(email, resetToken, expiry);
                LoggerUtil.logPasswordResetRequest(email, request);
            },
            () -> {
            }
        );

        request.setAttribute("success", "If an account with that email exists, a password reset link has been sent.");
        request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
    }

    private void handleResetTokenView(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        
        if (token == null || token.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/auth");
            return;
        }

        userDAO.findByResetToken(token).ifPresentOrElse(
            user -> {
                if (!user.isTokenExpired()) {
                    request.setAttribute("token", token);
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        request.setAttribute("error", "Reset token has expired");
                        request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                try {
                    request.setAttribute("error", "Invalid reset token");
                    request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void handleResetToken(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (!ValidationUtil.isNotEmpty(password) || !ValidationUtil.isNotEmpty(confirmPassword)) {
            request.setAttribute("token", token);
            request.setAttribute("error", "All fields are required");
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            request.setAttribute("token", token);
            request.setAttribute("error", "Password must be at least 8 characters");
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("token", token);
            request.setAttribute("error", "Passwords do not match");
            request.getRequestDispatcher("/WEB-INF/views/reset-password.jsp").forward(request, response);
            return;
        }

        userDAO.findByResetToken(token).ifPresentOrElse(
            user -> {
                if (!user.isTokenExpired()) {
                    userDAO.updatePassword(user.getId(), PasswordUtil.hashPassword(password));
                    LoggerUtil.logPasswordReset(user.getId(), request);
                    try {
                        request.setAttribute("success", "Password has been reset. Please log in.");
                        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        request.setAttribute("error", "Reset token has expired");
                        request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                try {
                    request.setAttribute("error", "Invalid reset token");
                    request.getRequestDispatcher("/WEB-INF/views/reset-request.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = SessionUtil.getUserId(session);
            LoggerUtil.logLogout(userId, request);
            SessionUtil.invalidate(session);
        }
        response.sendRedirect(request.getContextPath() + "/");
    }
}
