package com.loosenotes.web;

import com.loosenotes.model.ShareLink;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/share")
public class ShareViewServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getParameter("token");
        if (token == null || token.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Share link not found.");
            return;
        }
        ShareLink shareLink = app().getShareLinkDao().findByToken(token).orElse(null);
        if (shareLink == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Share link not found.");
            return;
        }
        redirect(request, response, "/notes/view?id=" + shareLink.getNoteId() + "&shareToken=" + token);
    }
}
