package fr.miage.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormatteurMessageTest {

    @Test
    void encoderRetourneUneChaineVidePourUneCommandeNulleOuVide() {
        assertEquals("", FormatteurMessage.encoder(null));
        assertEquals("", FormatteurMessage.encoder(new Commande(null)));
        assertEquals("", FormatteurMessage.encoder(new Commande(Map.of())));
    }

    @Test
    void encoderTransformeUneCommandeEnPayloadMqtt() {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(TypeLunette.BANANA, 2);
        lignes.put(TypeLunette.CLAUDE, 1);

        String payload = FormatteurMessage.encoder(new Commande(lignes));

        assertEquals("BANANA:2,CLAUDE:1", payload);
    }

    @Test
    void decoderTransformeUnPayloadMqttEnCommande() {
        Commande commande = FormatteurMessage.decoder("BANANA:2,CLAUDE:1");

        assertEquals(2, commande.lignes().get(TypeLunette.BANANA));
        assertEquals(1, commande.lignes().get(TypeLunette.CLAUDE));
        assertEquals(3, commande.getQuantiteTotale());
    }

    @Test
    void decoderAccepteLesEspacesEtLaCasseMinuscule() {
        Commande commande = FormatteurMessage.decoder(" banana : 2 , le_chat : 3 ");

        assertEquals(2, commande.lignes().get(TypeLunette.BANANA));
        assertEquals(3, commande.lignes().get(TypeLunette.LE_CHAT));
    }

    @Test
    void decoderIgnoreLesLignesInvalides() {
        Commande commande = FormatteurMessage.decoder("BANANA:2,INCONNU:4,CLAUDE:x,MALFORME");

        assertEquals(1, commande.lignes().size());
        assertEquals(2, commande.lignes().get(TypeLunette.BANANA));
    }

    @Test
    void decoderRetourneUneCommandeVidePourUnPayloadNulOuVide() {
        assertTrue(FormatteurMessage.decoder(null).lignes().isEmpty());
        assertTrue(FormatteurMessage.decoder("").lignes().isEmpty());
    }
}
