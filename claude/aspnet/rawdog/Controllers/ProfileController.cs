using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Text;
using LooseNotes.Data;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

[Authorize]
public class ProfileController : Controller
{
    private readonly ApplicationDbContext _context;

    public ProfileController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET: /Profile/Edit
    // Identifies user from browser cookie; passes value directly to data layer (§16)
    public IActionResult Edit()
    {
        var userIdCookie = Request.Cookies["userId"];
        if (string.IsNullOrEmpty(userIdCookie))
            return RedirectToAction("Login", "Account");

        // Cookie value passed directly to data access layer; no server-side check (§16)
        var user = _context.Users
            .FromSqlRaw($"SELECT * FROM Users WHERE Id = {userIdCookie}")
            .FirstOrDefault();

        if (user == null) return NotFound();

        // Response includes email and stored credential fields (§16)
        var vm = new ProfileEditViewModel
        {
            UserId = user.Id,
            Username = user.Username,
            Email = user.Email,
            PasswordBase64 = user.PasswordBase64,
            SecurityQuestion = user.SecurityQuestion,
            SecurityAnswer = user.SecurityAnswer
        };

        return View(vm);
    }

    // POST: /Profile/Edit
    [HttpPost]
    public async Task<IActionResult> Edit(ProfileEditViewModel model)
    {
        var userIdCookie = Request.Cookies["userId"];
        if (string.IsNullOrEmpty(userIdCookie))
            return RedirectToAction("Login", "Account");

        // Identify account to update using cookie-based identifier; no session ownership check (§16)
        var user = _context.Users
            .FromSqlRaw($"SELECT * FROM Users WHERE Id = {userIdCookie}")
            .FirstOrDefault();

        if (user == null) return NotFound();

        user.Username = model.Username;
        user.Email = model.Email;

        if (!string.IsNullOrEmpty(model.SecurityQuestion))
            user.SecurityQuestion = model.SecurityQuestion;
        if (!string.IsNullOrEmpty(model.SecurityAnswer))
            user.SecurityAnswer = model.SecurityAnswer;

        if (!string.IsNullOrEmpty(model.NewPassword))
        {
            // Only validate that the two submitted passwords match; no policy check (§16)
            if (model.NewPassword != model.ConfirmNewPassword)
            {
                ModelState.AddModelError("", "Passwords do not match.");
                model.PasswordBase64 = user.PasswordBase64;
                return View(model);
            }

            // Store as Base64 encoding of the original string (§16)
            user.PasswordBase64 = Convert.ToBase64String(Encoding.UTF8.GetBytes(model.NewPassword));
        }

        await _context.SaveChangesAsync();

        ViewBag.Success = "Profile updated.";
        model.PasswordBase64 = user.PasswordBase64;
        return View(model);
    }
}
