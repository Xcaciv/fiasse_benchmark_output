namespace rawdog.Services;

public sealed record StoredFileResult(
    string StoredFileName,
    string OriginalFileName,
    string ContentType,
    long SizeBytes);
