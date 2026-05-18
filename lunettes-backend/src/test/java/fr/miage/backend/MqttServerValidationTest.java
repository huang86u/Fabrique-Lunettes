package fr.miage.backend;

import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

public class MqttServerValidationTest {

    public static void main(String[] args) throws Exception {
        testerCommandeValide();
        testerCommandeVideInvalide();
        testerQuantiteTotaleNulleInvalide();
        testerQuantiteNegativeInvalide();
        testerQuantiteDixInvalide();
        System.out.println("Tous les tests MqttServerValidation sont passés.");
    }

    private static void testerCommandeValide() throws Exception {
        Commande commande = creerCommande(TypeLunette.BANANA, 2);

        verifier(validerCommande(commande), "Une commande avec une quantité entre 0 et 10 doit être valide");
    }

    private static void testerCommandeVideInvalide() throws Exception {
        Commande commande = new Commande(Map.of());

        verifier(!validerCommande(commande), "Une commande vide doit être invalide");
    }

    private static void testerQuantiteTotaleNulleInvalide() throws Exception {
        Commande commande = creerCommande(TypeLunette.BANANA, 0);

        verifier(!validerCommande(commande), "Une commande avec quantité totale nulle doit être invalide");
    }

    private static void testerQuantiteNegativeInvalide() throws Exception {
        Commande commande = creerCommande(TypeLunette.BANANA, -1);

        verifier(!validerCommande(commande), "Une quantité negative doit être invalide");
    }

    private static void testerQuantiteDixInvalide() throws Exception {
        Commande commande = creerCommande(TypeLunette.BANANA, 10);

        verifier(!validerCommande(commande), "Une quantité egale à 10 doit être invalide");
    }

    private static Commande creerCommande(TypeLunette type, int quantite) {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(type, quantite);
        return new Commande(lignes);
    }

    private static boolean validerCommande(Commande commande) throws Exception {
        MqttServer server = new MqttServer();
        Method methode = MqttServer.class.getDeclaredMethod("validerCommande", Commande.class);
        methode.setAccessible(true);
        return (boolean) methode.invoke(server, commande);
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
