using LooseNotes.Services;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

public class ShareController : Controller
{
    private readonly IShareLinkService _shareLinkService;
    private readonly ILogger<ShareController> _logger;

    public ShareController(IShareLinkService shareLinkService, ILogger<ShareController> logger)
    {
        _shareLinkService = shareLinkService;
        _logger = logger;
    }

    [HttpGet("share/{token}")]
    public async Task<IActionResult> View(string token)
    {
        if (string.IsNullOrWhiteSpace(token) || token.Length > 100)
        {
            return BadRequest("Invalid token.");
        }

        var note = await _shareLinkService.GetNoteByTokenAsync(token);

        if (note is null)
        {
            _logger.LogWarning("Invalid or inactive share token used.");
            return NotFound("Share link is invalid or has been revoked.");
        }

        return View("View", note);
    }
}
