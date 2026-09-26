package dev.nullapex.dragon.movement;

import java.util.Objects;

/** One managed routine tick: issue a command, pause custom steering, or finish the routine. */
public sealed interface FlightRoutineResult
    permits FlightRoutineResult.Command, FlightRoutineResult.Pause, FlightRoutineResult.Complete {

    static Command command(FlightCommand command) {
        return new Command(command);
    }

    static FlightRoutineResult pause() {
        return Pause.INSTANCE;
    }

    static FlightRoutineResult complete() {
        return Complete.INSTANCE;
    }

    record Command(FlightCommand command) implements FlightRoutineResult {
        public Command {
            Objects.requireNonNull(command, "command");
        }
    }

    enum Pause implements FlightRoutineResult {
        INSTANCE
    }

    enum Complete implements FlightRoutineResult {
        INSTANCE
    }
}
