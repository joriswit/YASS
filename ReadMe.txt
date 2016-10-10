=====================================================================
YASS - Yet Another Sokoban Solver and Optimizer - For Small Puzzles
Version 2.138 - October 2, 2016
Copyright (c) 2016 by Brian Damgaard, Denmark
E-mail: BrianDamgaard@jubii.dk
=====================================================================

Sokoban(r) Registered Trademark of Falcon Co., Ltd., Japan
Sokoban Copyright (c) 1982-2016 by Hiroyuki Imabayashi, Japan
Sokoban Copyright (c) 1989, 1990, 2001-2016 by Falcon Co., Ltd., Japan

License
--------
YASS - Yet Another Sokoban Solver and Optimizer
Copyright (c) 2016 by Brian Damgaard, Denmark

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.

Credits and acknowledgments
---------------------------
Many people have contributed to this program, and I'm particularly
indebted to:

Lee J Haywood for providing most of the basic deadlock patterns,
for sharing a lot of ideas, and for a long-standing correspondence
on Sokoban topics.

Matthias Meger for sharing a lot of ideas and for a long-standing
correspondence on solvers, optimizers, and deadlock-detection.

Sebastien Gouezel for inventing the "Vicinity search" optimization
method, and generously sharing information on the method and its
implementation. By sharing his ideas and insights on the subject,
he has made a significant and lasting contribution to the Sokoban
game itself, transcending the implementation of the algorithm in
the YASS optimizer.

Features
--------
The program offers two independent features:
* Search for solutions of Sokoban puzzles.
* Search for improvements of existing solutions.

Solving and optimizing Sokoban puzzles are complicated tasks for a 
computer program, so the program can only handle small puzzles.

The solver can find push-optimal solutions for some small puzzles,
whereas other puzzles may be solved without a push-optimality guarantee.

Please note that the found push-optimal solutions are not optimized
for moves, i.e., there may be other solutions with the same number of
pushes, but with fewer non-pushing moves.

Compiling the Program
---------------------
You need FPC 3.1.1 (development version) to compile PIC compatible shared
libraries. This is required by Android 6.0. 

The paths to the Android NDK must be configured in the Lazarus project
file (yassjni.lpi).
