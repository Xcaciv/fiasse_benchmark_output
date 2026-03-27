// HomeController.cs — Landing page and error handler.
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

public sealed class HomeController : Controller
{
    [HttpGet]
    [AllowAnonymous]
    public IActionResult Index() => View();

    [HttpGet]
    [AllowAnonymous]
    // Resilience: generic error view — stack trace never exposed to end users
    public IActionResult Error() => View();
}
