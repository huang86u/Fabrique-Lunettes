package fr.miage.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CommandeTest {

    @Test
    void getQuantiteTotaleAdditionneToutesLesLignes() {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(TypeLunette.BANANA, 2);
        lignes.put(TypeLunette.CHATGPT, 3);
        lignes.put(TypeLunette.CLAUDE, 1);

        Commande commande = new Commande(lignes);

        assertEquals(6, commande.getQuantiteTotale());
    }

    @Test
    void getQuantiteTotaleRetourneZeroPourUneCommandeVide() {
        Commande commande = new Commande(Map.of());

        assertEquals(0, commande.getQuantiteTotale());
    }
}
