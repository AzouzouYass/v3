# NetCourse V2 — projet Android (code source)

## Objectif
Analyser une offre **ouverte** dans inDrive, sans cliquer ni agir à la place du chauffeur.

- Prix et distance d'approche : lus grâce aux identifiants Android accessibles de l'offre ouverte.
- Distance du trajet client : étiquette **verte** sur la carte, lue par capture d'écran temporaire via `AccessibilityService.takeScreenshot` (Android 11+) et OCR local Google ML Kit.
- Étiquette **bleue** : OCR utilisé pour contrôler la distance d'approche déjà lue par accessibilité.
- Si l'étiquette verte est masquée ou illisible, affichage neutre « ANALYSE INCOMPLÈTE », **jamais « ACCEPTER »**.
- Pas de stockage/transmission des captures; aucune localisation demandée; aucune interaction automatique avec inDrive.

## Exemple fourni
Offre 16 DH; rectangle bleu 3,8 km; rectangle vert 3,9 km. Distance totale sans retour : 7,7 km. Avec les valeurs par défaut du projet (gazole 10,5 DH/L, consommation 5,2 L/100km, commission 10%, usure 0,40 DH/km, net cible 2 DH/km), calcul net ≈ 7,12 DH, soit 0,92 DH/km, prix minimum ≈ 26 DH. **Ces valeurs dépendent de tes propres réglages**.

## Compilation avec GitHub Actions (sans Android Studio)
1. Créer un dépôt GitHub privé.
2. Téléverser tout le contenu de cette archive **à la racine du dépôt**, y compris `.github/workflows/build-apk.yml`.
3. Ouvrir **Actions → Build APK → Run workflow** (ou pousser un commit).
4. Lorsque le build est vert, télécharger l'artefact **NetCourse-V2-apk**, l'extraire et installer `app-debug.apk`.
5. Attention : la signature de cet APK de développement ne sera probablement pas la même que celle de la version 1. Si Android refuse l'installation avec « application non installée », désinstaller l'ancienne NetCourse (les réglages seront perdus) puis installer la V2.
6. Sur Android 13+, si l'activation de l'accessibilité est grisée : **Infos appli → ⋮ → Autoriser les paramètres restreints**, puis activer Net course V2.

## Validation nécessaire
Le code n'a pas pu être compilé sur cette machine (Android SDK absent) ni exécuté sur un téléphone. Le premier build GitHub peut révéler une erreur de compilation; transmettre alors la section rouge du journal.

Les seuils de reconnaissance de rectangles ont été **validés sur la capture fournie** (bleu : x≈178–292,y≈340–436; vert : x≈334–446,y≈502–601, sur image 691×1536), mais ils ne sont pas encore testés sur plusieurs modèles de téléphones/zooms/orientations.

**Sécurité :** l'accès à l'écran est très puissant. Active ce service uniquement si tu fais confiance au code et n'utilise pas la bulle comme unique base de décision avant validation. Vérifie les conditions d'utilisation d'inDrive applicables à tes usages.
