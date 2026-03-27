package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.SessionUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isLoggedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/auth");
            return;
        }

        Long userId = SessionUtil.getUserId(session);
        userDAO.findById(userId).ifPresentOrElse(
            user -> {
                request.setAttribute("user", user);
                try {
                    request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/auth");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        
        if (!SessionUtil.isLoggedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/auth");
            return;
        }

        Long userId = SessionUtil.getUserId(session);
        String action = request.getParameter("action");

        if ("updateProfile".equals(action)) {
            updateProfile(request, response, userId);
        } else if ("updatePassword".equals(action)) {
            updatePassword(request, response, userId);
        } else {
            response.sendRedirect(request.getContextPath() + "/profile");
        }
    }

    private void updateProfile(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");

        if (!ValidationUtil.isNotEmpty(username) || !ValidationUtil.isNotEmpty(email)) {
            userDAO.findById(userId).ifPresent(user -> {
                request.setAttribute("user", user);
                request.setAttribute("error", "Username and email are required");
                try {
                    request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        if (!ValidationUtil.isValidUsername(username)) {
            userDAO.findById(userId).ifPresent(user -> {
                request.setAttribute("user", user);
                request.setAttribute("error", "Username must be 3-50 characters and contain only letters, numbers, and underscores");
                try {
                    request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            userDAO.findById(userId).ifPresent(user -> {
                request.setAttribute("user", user);
                request.setAttribute("error", "Please enter a valid email address");
                try {
                    request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        userDAO.findById(userId).ifPresentOrElse(
            user -> {
                if (!user.getUsername().equals(username) && userDAO.usernameExists(username)) {
                    request.setAttribute("user", user);
                    request.setAttribute("error", "Username already exists");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (!user.getEmail().equals(email) && userDAO.emailExists(email)) {
                    request.setAttribute("user", user);
                    request.setAttribute("error", "Email already exists");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                user.setUsername(username);
                user.setEmail(email);

                if (userDAO.updateProfile(user)) {
                    SessionUtil.setUser(request.getSession(false), user);
                    request.setAttribute("user", user);
                    request.setAttribute("success", "Profile updated successfully");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    request.setAttribute("user", user);
                    request.setAttribute("error", "Failed to update profile");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/auth");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void updatePassword(HttpServletRequest request, HttpServletResponse response, Long userId)
            throws ServletException, IOException {
        String currentPassword = request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (!ValidationUtil.isNotEmpty(currentPassword) || !ValidationUtil.isNotEmpty(newPassword) ||
            !ValidationUtil.isNotEmpty(confirmPassword)) {
            userDAO.findById(userId).ifPresent(user -> {
                request.setAttribute("user", user);
                request.setAttribute("passwordError", "All password fields are required");
                try {
                    request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return;
        }

        userDAO.findById(userId).ifPresentOrElse(
            user -> {
                if (!PasswordUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                    request.setAttribute("user", user);
                    request.setAttribute("passwordError", "Current password is incorrect");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (!ValidationUtil.isValidPassword(newPassword)) {
                    request.setAttribute("user", user);
                    request.setAttribute("passwordError", "New password must be at least 8 characters");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    request.setAttribute("user", user);
                    request.setAttribute("passwordError", "New passwords do not match");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                if (userDAO.updatePassword(userId, PasswordUtil.hashPassword(newPassword))) {
                    request.setAttribute("user", user);
                    request.setAttribute("passwordSuccess", "Password updated successfully");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    request.setAttribute("user", user);
                    request.setAttribute("passwordError", "Failed to update password");
                    try {
                        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/auth");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }
}
