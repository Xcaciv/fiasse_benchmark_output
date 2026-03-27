using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

public sealed class HomeController : Controller
{
    [HttpGet]
    public IActionResult Index()
    {
        if (User.Identity?.IsAuthenticated == true)
            return RedirectToAction("Index", "Notes");
        return View();
    }

    [HttpGet]
    public IActionResult Privacy() => View();

    [HttpGet]
    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        // Structured error page - no stack traces or internal details exposed (ASVS V16 / GSR-08)
        return View();
    }
}
