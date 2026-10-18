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
