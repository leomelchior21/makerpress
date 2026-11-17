# Paper activities - research and design

Research reviewed on 18 October 2026.

## References

- [Gladden Design: Paper Apps GOLF](https://gladdendesign.com/products/paper-apps-golf): procedurally generated pocket golf, played with pencil and a six-sided die.
- [Gladden Design: Paper Apps GALAXY](https://gladdendesign.com/products/paper-apps-galaxy): unique notebooks with space navigation, asteroids and enemy encounters.
- [The designer's explanation](https://gladdendesign.com/blogs/toylike/the-paper-apps%E2%84%A2-backstory): generation happens before printing; the player and die run the game on paper.
- [MEC: BNCC Ensino Médio](https://www.gov.br/mec/pt-br/cne/bncc_ensino_medio.pdf): algebraic/geometric representation of functions and modeling.
- [Rio Grande do Sul education department curriculum](https://educacao.rs.gov.br/upload/arquivos/202207/14142026-rcgem-jul-22.pdf): area, metric relations and solid geometry, including EM13MAT307-309.
- [IFTO curriculum](https://portal.ifto.edu.br/ifto/colegiados/consup/documentos-aprovados/ppc/campus-gurupi/fic-eja-em-cuidador-de-idoso-concomitante-por-intercomplementaridade-ao-ensino-medio/ppc-fic-em-cuidador-de-idoso-concomitante-eja-campusgurupi-ifto.pdf/@@download/file/ppc-fic-em-cuidador-de-idoso-concomitante-eja-campusgurupi-ifto.pdf): Newtonian mechanics related to EM13CNT101.

MakerPress uses original rules, vector symbols and generators. It does not embed Gladden PDFs, scans, logos or proprietary rulebook text. The printed games credit the inspiration and are not official Paper Apps products.

## Generation and printing

A seeded generator reserves a winding, connected path before clustering terrain. Golf has trees, sand and water. Galaxy has asteroids, enemies and three crystals on the reachable path. Movement permits one step on every die roll, so the guaranteed path is traversable under the printed rules. Galaxy's safe route avoids every enemy and visits all crystals. It has no turn limit; fewer turns is the scoring objective.

The preview is frozen. With **New level with each print** enabled, the first queued board is exactly the preview, each extra copy is a separate generated job, and the next preview advances to a fresh random seed. Saving without queuing does not change the board. Disable the toggle for identical copies. Requeuing history always reprints the stored level, and editing a historical game starts with this toggle off. Rendering and Bluetooth retries never generate random content.

Seeds use a 64-bit random source. Distinct seeds produce a huge practical variety, but randomness is not a mathematical promise that a layout can never recur. Level cells, route and generator version are serialized, so future generator changes cannot alter saved prints. Golf targets are suggestions, not calibrated pars. Print and play testing is still required to tune difficulty and ink density on actual printers.

## Study topics

| Topic | Exercises | BNCC reference |
| --- | --- | --- |
| Arithmetic | Four operations; integer division; difficulty ranges | Prerequisite review |
| Geometry | Garden area/perimeter, triangle area, Pythagorean ramp, box volume/surface | EM13MAT307, EM13MAT308, EM13MAT309 |
| Linear functions | Draw a line from its equation; recover equation and slope from plotted points | EM13MAT401 |
| Quadratic functions | Bhaskara/quadratic formula, roots, vertex; recover coefficients from roots and a third point | EM13MAT302, EM13MAT402 |
| Physics | Constant-speed car, approaching trains, falling ball, force/weight, kinetic energy/momentum | EM13CNT101; function modeling also EM13MAT302 |

These are selected practice activities associated with BNCC skills, not a complete curriculum or official certification. A teacher should choose the activity for their class. Physics states units, reference direction, gravity and idealizations. Diagrams are schematic; numerical labels govern dimensions. Graphs label axes, tick values and known points; graph-reading prompts do not reveal the target equation. Draw mode provides an empty coordinate plane, while the answer key plots the solution. Mixed review rotates subjects; alternate mode alternates drawing and reading. Each question has working space and a separate worked answer key.

## Language

The header switch and Settings persist PT-BR / EN in DataStore. Study prompts, solutions, game rules, navigation and controls follow the selection. New built-in word lists follow it as well; custom words and user-authored content are preserved. Current editable previews regenerate with the same seed when language changes. Historical documents retain their original content and language until explicitly edited. Technical Bluetooth/provider error details may retain their original language.

## Kakooma-style puzzles

[Tang Math's official Kakooma Plus description](https://store.tangmath.com/products/kakooma-plus-1-pack) describes locating a trio where two numbers sum to the third. MakerPress generates original addition groups of 5, 7 or 9 distinct numbers. It enumerates every unordered operand pair and every different target position; a group is accepted only if exactly one equation exists. Distractors and number order vary by seed. Choose 5, 10 or 15 groups and print the answer key separately. This is an unofficial implementation with printed attribution, not a reproduction of Tang Math cards.

## Connect the dots

Six original closed silhouettes: rocket, fish, cat, butterfly, house and sailboat. Surprise chooses one by seed. Difficulty controls edge subdivision and therefore dot count. Scaling, mirroring, starting point and direction vary while preserving the ordered outline. The last point reconnects to 1. The answer key draws the lines and names the subject. The requested selection is stored separately from the revealed subject so editing a saved Surprise drawing preserves its exact seed behavior.

## 1.2.0: map design, new activities and classroom tools

### Printing shades on a 1-bit head

Thermal printers have no gray. Each terrain is a hand-made dot pattern at the printer's native pitch: fairway (sparse dots), green (denser dots), sand (diagonal hatching), water (black with white waves), nebula (stipple), asteroid (dark speckle) and dungeon wall (cross-hatching). Areas are drawn as a metaball union of their grid cells, so neighbouring cells merge into rounded shapes instead of separate symbols. Green, sand, water and asteroid areas get a 1-dot outline. Grid dots get a white halo so they stay readable on every texture. Pattern heat bleed varies between printers: print **Printer Lab → Calibrate map textures** and pick lighter, normal or darker under **Map textures** in the print profile.

### Golf

The generator lays a curved tee-to-green line. Along it go a fairway with carries (rough gaps), a green around the cup, bunkers beside the green and at the landing zone, a creek or pond, and forest clusters kept off the playing line. Rules: roll and move up to the roll in a straight line in 8 directions. The ball flies over everything except trees. The lie caps the next roll: fairway 6, rough 4, sand 2, green 2. Water returns the ball to its previous dot and adds a stroke, and the ball must finish exactly on the cup.

**Calculated par.** Value iteration over the real dice odds gives the expected number of strokes from the tee with optimal play. That value, rounded, is the printed par, and the exact figure is printed beside the stroke tracker. Holes come in three lengths (13, 19 and 25 rows), and longer holes have a higher par. A hole whose expected strokes exceed 25 is rejected and regenerated, so every printed hole is playable. Generators use `StrictMath`, so a share code produces identical boards on every phone.

**Course mode** prints 3, 6 or 9 holes mixing the three lengths, with one key and one set of rules, followed by a scorecard (par row plus 1–4 player rows and totals).

### Galaxy

A reserved winding route keeps every crystal and the portal reachable. Asteroid fields never cover the route. Nebulas may cover it (stop on entry), and enemies sit off the route. The target is the shortest enemy-free tour through all crystals divided by an average roll of 3.5, plus nebula stops.

### Dungeon

Rooms are joined by corridors. The stairs room has exactly one entrance, which becomes the locked door, and the generator verifies that the key is reachable without passing the door. Monsters show their strength (2–6): roll that number or more to defeat them. On medium and hard a guard stands at the door. Traps, chests and potions complete the room. Score is treasure × 2 + hearts left.

### Maze, Nonogram and Bingo

- **Maze:** a randomized depth-first search produces a perfect maze (exactly one path between any two cells). Optional stars sit in dead ends off the solution path. The answer key draws the path.
- **Nonogram:** 12 original 10 × 10 pictures and 7 original 15 × 15 pictures. A line solver proves that each one can be finished by pure logic, which implies a unique solution. Mirroring adds variety. Easy adds 6 pre-filled cells.
- **Bingo:** numbers 1–75 (B-I-N-G-O columns, free centre), times tables (cards show products; the caller reads facts such as 7 × 8) or words. Word cards are 5 × 5 with at least 24 words and 3 × 3 with 9–23. The answer-key button prints the caller list.

### Codes, daily challenge and class mode

A share code is a tool letter, a difficulty letter (E/M/H), one option character and a 30-bit seed in Crockford base32, for example `GM0-7K2QX9`. Typing O for 0 or I/L for 1 is tolerated. The daily seed is a hash of the tool and the date, so it needs no server. Class mode queues one different sheet per student, numbered so the teacher can match it to the combined answer key. Keys are packed into as few prints as the length limit allows, and study answers use a compact one-line form.
