package abc;

import akka.Done;
import akka.actor.CoordinatedShutdown;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.AskPattern;
import akka.japi.function.Function;
import akka.japi.function.Function2;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletionStage;


public abstract class TypedActor<Command, Reply> extends AbstractBehavior<Command> {

    protected final Logger _log;
    protected final ActorContext<Command> _context;

    public TypedActor(final ActorContext<Command> context, final Logger log) {
        super(context);
        _context = getContext();
        _log = log;
    }

    protected ActorRef<Command> self() {
        return _context.getSelf();
    }


    protected <Value> void pipeToSelf(final CompletionStage<Value> future, final Function2<Value, Throwable, Command> applyToResult) {
        getContext().pipeToSelf(future, applyToResult);
    }

    protected CompletionStage<Reply> askSelf(final Function<ActorRef<Reply>, Command> messageFactory) {
        return AskPattern.ask(
                self(),
                messageFactory,
                Duration.ofSeconds(10),
                getContext().getSystem().scheduler()
        );
    }

    protected <OtherCommand, OtherReply> CompletionStage<OtherReply> askOther(final ActorRef<OtherCommand> other, final Function<ActorRef<OtherReply>, OtherCommand> messageFactory) {
        return AskPattern.ask(
                other,
                messageFactory,
                Duration.ofSeconds(10),
                getContext().getSystem().scheduler()
        );
    }

    protected void coordinatedShutdown(final String name, final Function<ActorRef<Done>, Command> messageFactory) {
        CoordinatedShutdown.get(getContext().getSystem())
                .addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind(), name,
                        () -> AskPattern.ask(self(), messageFactory, Duration.ofSeconds(5), getContext().getSystem().scheduler())
                );
    }
}
