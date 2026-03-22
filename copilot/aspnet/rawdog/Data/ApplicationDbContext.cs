using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using rawdog.Models;

namespace rawdog.Data;

public sealed class ApplicationDbContext(DbContextOptions<ApplicationDbContext> options)
    : IdentityDbContext<ApplicationUser>(options)
{
    public DbSet<Note> Notes => Set<Note>();

    public DbSet<NoteAttachment> Attachments => Set<NoteAttachment>();

    public DbSet<NoteRating> Ratings => Set<NoteRating>();

    public DbSet<NoteShareLink> ShareLinks => Set<NoteShareLink>();

    public DbSet<ActivityLog> ActivityLogs => Set<ActivityLog>();

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);

        builder.Entity<ApplicationUser>()
            .HasIndex(user => user.RegisteredAtUtc);

        builder.Entity<Note>()
            .HasIndex(note => note.OwnerId);

        builder.Entity<Note>()
            .HasIndex(note => note.IsPublic);

        builder.Entity<Note>()
            .HasOne(note => note.Owner)
            .WithMany(user => user.Notes)
            .HasForeignKey(note => note.OwnerId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<NoteAttachment>()
            .HasIndex(attachment => attachment.NoteId);

        builder.Entity<NoteAttachment>()
            .HasOne(attachment => attachment.Note)
            .WithMany(note => note.Attachments)
            .HasForeignKey(attachment => attachment.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<NoteRating>()
            .HasIndex(rating => new { rating.NoteId, rating.UserId })
            .IsUnique();

        builder.Entity<NoteRating>()
            .HasOne(rating => rating.Note)
            .WithMany(note => note.Ratings)
            .HasForeignKey(rating => rating.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<NoteRating>()
            .HasOne(rating => rating.User)
            .WithMany(user => user.Ratings)
            .HasForeignKey(rating => rating.UserId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<NoteShareLink>()
            .HasIndex(link => link.Token)
            .IsUnique();

        builder.Entity<NoteShareLink>()
            .HasOne(link => link.Note)
            .WithMany(note => note.ShareLinks)
            .HasForeignKey(link => link.NoteId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Entity<ActivityLog>()
            .HasIndex(log => log.CreatedAtUtc);
    }
}
