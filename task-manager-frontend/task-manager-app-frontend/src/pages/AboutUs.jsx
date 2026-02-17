import { useEffect, useRef } from "react";
import { FiUsers, FiCheckCircle, FiShield, FiZap } from "react-icons/fi";
import "@styles/pages/AboutUs.css";

const FEATURES = [
    {
        icon: <FiUsers size={28} />,
        title: "Team collaboration",
        text: "Organise your work into groups, assign tasks, and keep everyone on the same page.",
    },
    {
        icon: <FiCheckCircle size={28} />,
        title: "Task tracking",
        text: "Track progress from TODO to DONE with built-in review workflows and priorities.",
    },
    {
        icon: <FiShield size={28} />,
        title: "Role-based access",
        text: "Fine-grained roles — Guest, Member, Reviewer, Task Manager, Leader — so the right people see the right things.",
    },
    {
        icon: <FiZap size={28} />,
        title: "Real-time updates",
        text: "Smart delta-polling keeps dashboards fresh without constant page reloads.",
    },
];

export default function AboutUs() {
    const cardsRef = useRef([]);

    // Fade-in on scroll (Intersection Observer)
    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add("about-visible");
                        observer.unobserve(entry.target);
                    }
                });
            },
            { threshold: 0.15 }
        );

        cardsRef.current.forEach((el) => el && observer.observe(el));
        return () => observer.disconnect();
    }, []);

    return (
        <div className="about-page">
            {/* Hero */}
            <section className="about-hero">
                <h1 className="about-hero-title">Task Manager</h1>
                <p className="about-hero-sub">
                    A collaborative task-management platform built for teams that value
                    clarity, accountability, and speed.
                </p>
            </section>

            {/* Story placeholder */}
            <section className="about-story">
                <h2>Our story</h2>
                <p>
                    {/* ── PUT YOUR TEXT HERE ── */}
                    This project was created as part of a university thesis exploring
                    modern full-stack development with React, Spring Boot, and Azure
                    cloud services. The goal was to build a production-grade tool that
                    real teams could use day-to-day.
                </p>
                <p>
                    {/* ── PUT MORE TEXT HERE ── */}
                    We believe great software comes from understanding real workflows.
                    Every feature — from role-based permissions to smart caching — was
                    designed with actual collaboration pain-points in mind.
                </p>
            </section>

            {/* Features */}
            <section className="about-features">
                <h2>What we offer</h2>
                <div className="about-features-grid">
                    {FEATURES.map((f, i) => (
                        <div
                            key={i}
                            className="about-feature-card"
                            ref={(el) => (cardsRef.current[i] = el)}
                        >
                            <div className="about-feature-icon">{f.icon}</div>
                            <h3>{f.title}</h3>
                            <p>{f.text}</p>
                        </div>
                    ))}
                </div>
            </section>

            {/* Contact / placeholder */}
            <section className="about-contact">
                <h2>Get in touch</h2>
                <p>
                    {/* ── PUT CONTACT INFO OR LINKS HERE ── */}
                    Have questions, feedback, or want to contribute? Reach out via the
                    project repository or email us at&nbsp;
                    <strong>placeholder@example.com</strong>.
                </p>
            </section>
        </div>
    );
}