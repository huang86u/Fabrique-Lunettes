package fr.miage.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MqttServerValidationTest {

    @Test
    void validerCommandeAccepteUneQuantiteEntreUnEtNeuf() throws Exception {
        assertTrue(valider(creerCommande(TypeLunette.BANANA, 1)));
        assertTrue(valider(creerCommande(TypeLunette.CLAUDE, 9)));
    }

    @Test
    void validerCommandeRefuseUneCommandeVide() throws Exception {
        assertFalse(valider(new Commande(Map.of())));
    }

    @Test
    void validerCommandeRefuseUneQuantiteTotaleNulle() throws Exception {
        assertFalse(valider(creerCommande(TypeLunette.CHATGPT, 0)));
    }

    @Test
    void validerCommandeRefuseUneQuantiteNegative() throws Exception {
        assertFalse(valider(creerCommande(TypeLunette.LE_CHAT, -1)));
    }

    @Test
    void validerCommandeRefuseUneQuantiteEgaleOuSuperieureADix() throws Exception {
        assertFalse(valider(creerCommande(TypeLunette.BANANA, 10)));
        assertFalse(valider(creerCommande(TypeLunette.BANANA, 12)));
    }

    @Test
    void validerCommandeRefuseUneCommandeMixteAvecUneLigneInvalide() throws Exception {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(TypeLunette.BANANA, 2);
        lignes.put(TypeLunette.CLAUDE, 10);

        assertFalse(valider(new Commande(lignes)));
    }

    private static Commande creerCommande(TypeLunette type, int quantite) {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(type, quantite);
        return new Commande(lignes);
    }

    private static boolean valider(Commande commande) throws Exception {
        MqttServer serveur = new MqttServer();
        Method methode = MqttServer.class.getDeclaredMethod("validerCommande", Commande.class);
        methode.setAccessible(true);
        return (boolean) methode.invoke(serveur, commande);
    }
}
