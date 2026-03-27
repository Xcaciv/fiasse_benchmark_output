using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Data;

public class ApplicationDbContext : IdentityDbContext<ApplicationUser>
{
    public ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
        : base(options)
    {
    }

    public DbSet<Note> Notes => Set<Note>();
    public DbSet<Attachment> Attachments => Set<Attachment>();
    public DbSet<Rating> Ratings => Set<Rating>();
    public DbSet<ShareLink> ShareLinks => Set<ShareLink>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<Note>(e =>
        {
            e.HasIndex(n => n.UserId);
            e.HasOne(n => n.User)
             .WithMany(u => u.Notes)
             .HasForeignKey(n => n.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Attachment>(e =>
        {
            e.HasOne(a => a.Note)
             .WithMany(n => n.Attachments)
             .HasForeignKey(a => a.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<Rating>(e =>
        {
            e.HasOne(r => r.Note)
             .WithMany(n => n.Ratings)
             .HasForeignKey(r => r.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
            e.HasOne(r => r.User)
             .WithMany(u => u.Ratings)
             .HasForeignKey(r => r.UserId)
             .OnDelete(DeleteBehavior.Cascade);
        });

        builder.Entity<ShareLink>(e =>
        {
            e.HasIndex(s => s.Token).IsUnique();
            e.HasOne(s => s.Note)
             .WithMany(n => n.ShareLinks)
             .HasForeignKey(s => s.NoteId)
             .OnDelete(DeleteBehavior.Cascade);
        });
    }
}
