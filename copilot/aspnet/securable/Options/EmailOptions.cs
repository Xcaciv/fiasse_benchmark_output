namespace LooseNotes.Options;

public sealed class EmailOptions
{
    public string OutboxPath { get; set; } = Path.Combine("App_Data", "outbox");
}
