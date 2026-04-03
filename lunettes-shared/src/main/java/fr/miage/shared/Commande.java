package fr.miage.shared;
import java.util.Map;

public record Commande(Map<TypeLunette, Integer> lignes) {
    public int getQuantiteTotale() {
        return lignes.values().stream().mapToInt(Integer::intValue).sum();
    }
}