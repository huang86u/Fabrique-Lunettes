package fr.miage.shared;
import java.util.Map;

//Commande passée par le client
public record Commande(Map<TypeLunette, Integer> lignes) {
    public int getQuantiteTotale() {
        return lignes.values().stream().mapToInt(Integer::intValue).sum();
    }
}