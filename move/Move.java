/*
 * Copyright 2017 riddles.io (developers@riddles.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     For the full copyright and license information, please view the LICENSE
 *     file that was distributed with this source code.
 */

package move;

/**
 * move.Move
 *
 * Used to output a move to the engine
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class Move {

    private MoveType moveType = MoveType.PASS;
    private Integer bombTicks;

    public Move() {}

    public Move(MoveType moveType) {
        this.moveType = moveType;
        this.bombTicks = null;
    }

    public Move(MoveType moveType, int bombTicks) {
        this.moveType = moveType;
        this.bombTicks = bombTicks;
    }

    public String toString() {
        if (this.moveType == MoveType.PASS || this.bombTicks == null) {
            return this.moveType.toString();
        }

        return String.format("%s;drop_bomb %d", this.moveType, this.bombTicks);
    }
}
