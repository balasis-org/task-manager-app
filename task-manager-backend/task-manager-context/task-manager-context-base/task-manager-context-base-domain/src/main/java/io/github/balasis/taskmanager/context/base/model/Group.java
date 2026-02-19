package io.github.balasis.taskmanager.context.base.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Groups" , uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name","owner_id"})
})
public class Group extends BaseModel{

    @Column(name="name", nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 150)
    private String Announcement;

    @Column(length = 500)
    private String defaultImgUrl;

    @Column(length = 500)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Task> tasks = new HashSet<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<GroupEvent> groupEvent = new HashSet<>();

    @Column
    private Instant lastGroupEventDate;

    @Column
    @Builder.Default
    private Boolean allowEmailNotification = true;

    @Column
    private Instant lastChangeInGroup;

    @Column
    private Instant lastChangeInGroupNoJoins;

    @Column
    private Instant lastDeleteTaskDate;

    @Column
    private Instant lastMemberChangeDate;

    @Column
    private Instant lastMaintenanceDate;


    @Column
    private Instant createdAt;

    @PrePersist
    protected void onCreate(){
        createdAt = Instant.now();
    }
}
