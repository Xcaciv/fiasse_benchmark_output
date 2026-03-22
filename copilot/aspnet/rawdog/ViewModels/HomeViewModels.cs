namespace rawdog.ViewModels;

public sealed class HomeIndexViewModel
{
    public int UserCount { get; set; }

    public int PublicNoteCount { get; set; }

    public int RatingCount { get; set; }

    public IReadOnlyList<SearchResultItemViewModel> LatestPublicNotes { get; set; } = Array.Empty<SearchResultItemViewModel>();

    public IReadOnlyList<TopRatedNoteViewModel> TopRatedNotes { get; set; } = Array.Empty<TopRatedNoteViewModel>();
}

public sealed class SearchViewModel
{
    public string Query { get; set; } = string.Empty;

    public IReadOnlyList<SearchResultItemViewModel> Results { get; set; } = Array.Empty<SearchResultItemViewModel>();
}

public sealed class SearchResultItemViewModel
{
    public int Id { get; set; }

    public string Title { get; set; } = string.Empty;

    public string Excerpt { get; set; } = string.Empty;

    public string Author { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; }

    public bool IsPublic { get; set; }
}

public sealed class TopRatedNoteViewModel
{
    public int Id { get; set; }

    public string Title { get; set; } = string.Empty;

    public string Author { get; set; } = string.Empty;

    public string Preview { get; set; } = string.Empty;

    public double AverageRating { get; set; }

    public int RatingCount { get; set; }
}
