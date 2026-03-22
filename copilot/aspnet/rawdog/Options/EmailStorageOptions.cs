namespace rawdog.Options;

public sealed class EmailStorageOptions
{
    public const string SectionName = "EmailStorage";

    public string RootPath { get; set; } = "App_Data\\emails";
}
