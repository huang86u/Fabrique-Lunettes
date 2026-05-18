package fr.miage.shared;

import java.util.EnumMap;
import java.util.Map;

public class FormatteurMessageTest {

    public static void main(String[] args) {
        testerEncodageCommande();
        testerDecodageCommande();
        testerCommandeVide();
        testerLigneInvalideIgnoree();
        System.out.println("Tous les tests FormatteurMessage sont passés.");
    }

    private static void testerEncodageCommande() {
        Map<TypeLunette, Integer> lignes = new EnumMap<>(TypeLunette.class);
        lignes.put(TypeLunette.BANANA, 2);
        lignes.put(TypeLunette.CLAUDE, 1);

        String payload = FormatteurMessage.encoder(new Commande(lignes));

        verifier(payload.contains("BANANA:2"), "La commande encodée doit contenir BANANA:2");
        verifier(payload.contains("CLAUDE:1"), "La commande encodée doit contenir CLAUDE:1");
        verifier(!payload.endsWith(","), "La commande encodée ne doit pas finir par une virgule");
    }

    private static void testerDecodageCommande() {
        Commande commande = FormatteurMessage.decoder("BANANA:2,CLAUDE:1");

        verifier(commande.lignes().get(TypeLunette.BANANA) == 2, "BANANA doit avoir une quantité de 2");
        verifier(commande.lignes().get(TypeLunette.CLAUDE) == 1, "CLAUDE doit avoir une quantité de 1");
        verifier(commande.getQuantiteTotale() == 3, "La quantité totale doit être 3");
    }

    private static void testerCommandeVide() {
        verifier(FormatteurMessage.encoder(new Commande(Map.of())).isEmpty(), "Une commande vide doit donner un payload vide");
        verifier(FormatteurMessage.decoder("").lignes().isEmpty(), "Un payload vide doit donner une commande vide");
    }

    private static void testerLigneInvalideIgnoree() {
        Commande commande = FormatteurMessage.decoder("BANANA:2,INCONNU:4,CLAUDE:1");

        verifier(commande.lignes().size() == 2, "La ligne avec un type inconnu doit être ignorée");
        verifier(commande.lignes().containsKey(TypeLunette.BANANA), "BANANA doit être conservé");
        verifier(commande.lignes().containsKey(TypeLunette.CLAUDE), "CLAUDE doit être conservé");
    }

    private static void verifier(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
