using LooseNotes.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles public share-link access. No authentication required,
/// but token validity is strictly enforced (Authenticity + Integrity).
/// </summary>
[Route("[controller]/[action]")]
public class ShareController : Controller
{
    private readonly IShareLinkService _shareLinkService;
    private readonly ILogger<ShareController> _logger;

    public ShareController(IShareLinkService shareLinkService, ILogger<ShareController> logger)
    {
        _shareLinkService = shareLinkService;
        _logger = logger;
    }

    /// <summary>
    /// Trust boundary: token is an untrusted user-supplied value.
    /// Validate length, then resolve through service (Integrity).
    /// </summary>
    [HttpGet]
    public async Task<IActionResult> ViewNote(string token)
    {
        _logger.LogInformation("Share/View called with token length={Len}", token?.Length ?? 0);

        // Basic sanity check before hitting DB (Integrity)
        if (string.IsNullOrWhiteSpace(token) || token.Length > 64)
        {
            return NotFound();
        }

        var note = await _shareLinkService.GetNoteByShareTokenAsync(token);
        if (note is null) return NotFound("Share link not found, revoked, or expired.");

        return View("View", note);
    }
}
