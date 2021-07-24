package guru.springframework.msscssm.config;

import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.service.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigBuilder;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;
import java.util.Random;

@Slf4j
@EnableStateMachineFactory
@Configuration
public class StateMachineConfig extends StateMachineConfigurerAdapter<PaymentState, PaymentEvent> {


    @Override
    public void configure(StateMachineStateConfigurer<PaymentState, PaymentEvent> states) throws Exception {
        states.withStates()
                .initial(PaymentState.NEW)
                .states(EnumSet.allOf(PaymentState.class))
                .end(PaymentState.AUTH)
                .end(PaymentState.PRE_AUTH_ERROR)
                .end(PaymentState.AUTH_ERROR);
        //todo states - Ну это понятно нужно дать ему все статусы какие могут быть вообще
        //todo initial - нужно указать для sm начало статуса этого обьекта как бы с чего всегда начинается статус когда обьект рождается.
        //todo end- это концы статусов. Статус этого обьекта может окончиться в трех вариантах AUHT - (OK),PRE_AUTH_ERROR - (Ошибка 1), AUTH_ERROR (Ошибка 2)
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<PaymentState, PaymentEvent> transitions) throws Exception {
        //todo конфигурируем все действия и переходы.
        transitions.withExternal()
                .source(PaymentState.NEW)
                .target(PaymentState.NEW)
                .event(PaymentEvent.PRE_AUTHORIZE)
                .action(preAuthAction()) //todo действие которое произойдет при переходе с source в target
        .and()
        .withExternal()
                .source(PaymentState.NEW)
                .target(PaymentState.PRE_AUTH)
                .event(PaymentEvent.PRE_AUTH_APPROVED)
        .and()
        .withExternal()
                .source(PaymentState.NEW)
                .target(PaymentState.PRE_AUTH_ERROR)
                .event(PaymentEvent.PRE_AUTH_DECLINED);
    }


    @Override
    public void configure(StateMachineConfigurationConfigurer<PaymentState, PaymentEvent> config) throws Exception {
        StateMachineListener<PaymentState,PaymentEvent> adapter = new StateMachineListenerAdapter<>(){
            @Override
            public void stateChanged(State<PaymentState, PaymentEvent> from, State<PaymentState, PaymentEvent> to) {
                log.info("StateChanged(from: {}, to: {}",from,to);
            }
        };
        config.withConfiguration().listener(adapter);
    }

    public Action<PaymentState, PaymentEvent> preAuthAction(){
        return stateContext -> {
            System.out.println("PreAuth was called!!!");
        //todo просто пример поэтому Random!
            if (new Random().nextInt(10) < 8){
                System.out.println("Approved!");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_APPROVED)
                .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                .build());
            }else {
                System.out.println("Declined NO CREDIT !!");
                stateContext.getStateMachine().sendEvent(MessageBuilder.withPayload(PaymentEvent.PRE_AUTH_DECLINED)
                        .setHeader(PaymentServiceImpl.PAYMENT_ID_HEADER, stateContext.getMessageHeader(PaymentServiceImpl.PAYMENT_ID_HEADER))
                        .build());
            }
        };
    }


}
