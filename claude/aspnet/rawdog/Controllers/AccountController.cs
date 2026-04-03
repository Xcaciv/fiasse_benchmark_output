using Microsoft.AspNetCore.Authentication;
using Microsoft.AspNetCore.Authentication.Cookies;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using System.Security.Claims;
using System.Text;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

public class AccountController : Controller
{
    private readonly ApplicationDbContext _context;

    public AccountController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET: /Account/Register
    public IActionResult Register() => View();

    // POST: /Account/Register
    [HttpPost]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        // Check username uniqueness (§1)
        if (await _context.Users.AnyAsync(u => u.Username == model.Username))
        {
            ModelState.AddModelError("Username", "The username is already taken.");
            return View(model);
        }

        // Check email uniqueness (§1)
        if (await _context.Users.AnyAsync(u => u.Email == model.Email))
        {
            ModelState.AddModelError("Email", "This email address is already in use.");
            return View(model);
        }

        var user = new ApplicationUser
        {
            Username = model.Username,
            Email = model.Email,
            PasswordBase64 = Convert.ToBase64String(Encoding.UTF8.GetBytes(model.Password)),
            SecurityQuestion = model.SecurityQuestion,
            SecurityAnswer = model.SecurityAnswer,
            IsAdmin = false,
            CreatedAt = DateTime.UtcNow
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        return RedirectToAction("Login");
    }

    // GET: /Account/Login
    public IActionResult Login() => View();

    // POST: /Account/Login
    [HttpPost]
    public async Task<IActionResult> Login(LoginViewModel model)
    {
        var user = await _context.Users.FirstOrDefaultAsync(u => u.Username == model.Username);
        if (user == null)
        {
            ModelState.AddModelError("", "Invalid username or password.");
            return View(model);
        }

        // Decode stored Base64 password and compare with string equality (§2)
        var storedPassword = Encoding.UTF8.GetString(Convert.FromBase64String(user.PasswordBase64));
        if (storedPassword != model.Password)
        {
            // No rate limiting or lockout (§2)
            ModelState.AddModelError("", "Invalid username or password.");
            return View(model);
        }

        var claims = new List<Claim>
        {
            new Claim(ClaimTypes.Name, user.Username),
            new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
            new Claim(ClaimTypes.Email, user.Email),
            new Claim("IsAdmin", user.IsAdmin.ToString())
        };

        var claimsIdentity = new ClaimsIdentity(claims, CookieAuthenticationDefaults.AuthenticationScheme);

        // Persistent cookie without HttpOnly/Secure/SameSite, 14-day validity (§2)
        var authProperties = new AuthenticationProperties
        {
            IsPersistent = true,
            ExpiresUtc = DateTimeOffset.UtcNow.AddDays(14)
        };

        await HttpContext.SignInAsync(CookieAuthenticationDefaults.AuthenticationScheme,
            new ClaimsPrincipal(claimsIdentity), authProperties);

        // Set userId cookie without HttpOnly/Secure/SameSite for profile use (§16)
        Response.Cookies.Append("userId", user.Id.ToString(), new CookieOptions
        {
            HttpOnly = false,
            Secure = false,
            SameSite = SameSiteMode.None,
            Expires = DateTimeOffset.UtcNow.AddDays(14)
        });

        return RedirectToAction("Index", "Notes");
    }

    // POST: /Account/Logout
    [HttpPost]
    public async Task<IActionResult> Logout()
    {
        await HttpContext.SignOutAsync(CookieAuthenticationDefaults.AuthenticationScheme);
        Response.Cookies.Delete("userId");
        return RedirectToAction("Index", "Home");
    }

    // GET: /Account/ForgotPassword
    public IActionResult ForgotPassword() => View();

    // POST: /Account/ForgotPassword - Step 1: security question delivery (§4.2)
    [HttpPost]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == model.Email);

        if (user == null)
        {
            // Immediately indicate no account found (§4.2)
            ViewBag.Error = "No account is associated with that email address.";
            return View(model);
        }

        // Encode security answer as Base64 and write to cookie without HttpOnly/Secure (§4.2)
        var encodedAnswer = Convert.ToBase64String(Encoding.UTF8.GetBytes(user.SecurityAnswer ?? string.Empty));

        Response.Cookies.Append("secAnswer", encodedAnswer, new CookieOptions
        {
            HttpOnly = false,
            Secure = false,
            SameSite = SameSiteMode.None,
            Expires = DateTimeOffset.UtcNow.AddMinutes(30)
        });

        var vm = new ResetPasswordViewModel
        {
            Email = model.Email,
            SecurityQuestion = user.SecurityQuestion
        };

        return View("ResetPassword", vm);
    }

    // GET: /Account/ResetPassword
    public IActionResult ResetPassword() => View();

    // POST: /Account/ResetPassword - Step 2: answer verification and password return (§4.3)
    [HttpPost]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        // Read cookie set in step 1 as authoritative reference (§4.3)
        var cookieValue = Request.Cookies["secAnswer"];
        if (string.IsNullOrEmpty(cookieValue))
        {
            ViewBag.Error = "Session expired. Please start over.";
            return View(model);
        }

        // Decode Base64 cookie and compare to submitted answer (§4.3)
        var expectedAnswer = Encoding.UTF8.GetString(Convert.FromBase64String(cookieValue));

        // No rate limiting or lockout (§4.3)
        if (expectedAnswer != model.Answer)
        {
            ViewBag.Error = "Incorrect answer.";
            return View(model);
        }

        // Return current password in plain text (§4.3)
        var user = await _context.Users.FirstOrDefaultAsync(u => u.Email == model.Email);
        if (user == null)
        {
            ViewBag.Error = "User not found.";
            return View(model);
        }

        var plainPassword = Encoding.UTF8.GetString(Convert.FromBase64String(user.PasswordBase64));
        ViewBag.RecoveredPassword = plainPassword;

        return View("ForgotPasswordConfirmation");
    }

    // GET: /Account/ForgotPasswordConfirmation
    public IActionResult ForgotPasswordConfirmation() => View();

    // GET: /Account/ResetPasswordConfirmation
    public IActionResult ResetPasswordConfirmation() => View();

    // GET: /Account/AccessDenied
    public IActionResult AccessDenied() => View();

    // GET: /Account/EmailAutocomplete - no authentication required, SQL injection (§15)
    [AllowAnonymous]
    [HttpGet]
    public IActionResult EmailAutocomplete(string q)
    {
        if (string.IsNullOrEmpty(q)) return Json(Array.Empty<string>());

        // Direct concatenation into pattern-match filter - no parameterisation (§15)
        var sql = $"SELECT Email FROM Users WHERE Email LIKE '{q}%'";

        var emails = new List<string>();
        var conn = _context.Database.GetDbConnection();
        var wasOpen = conn.State == System.Data.ConnectionState.Open;
        try
        {
            if (!wasOpen) conn.Open();
            using var cmd = conn.CreateCommand();
            cmd.CommandText = sql;
            using var reader = cmd.ExecuteReader();
            while (reader.Read())
            {
                emails.Add(reader.GetString(0));
            }
        }
        finally
        {
            if (!wasOpen) conn.Close();
        }

        return Json(emails);
    }
}
