package fr.miage.client;

import fr.miage.shared.TypeLunette;

public record ProductDefinition(
        TypeLunette type,
        String displayName,
        double price,
        String badge,
        String description
) {
}
