package nl.dennisschroer.messagestub.controller;

import lombok.extern.apachecommons.CommonsLog;
import nl.dennisschroer.messagestub.message.Message;
import nl.dennisschroer.messagestub.message.MessageService;
import nl.dennisschroer.messagestub.message.action.MessageAction;
import nl.dennisschroer.messagestub.message.action.MessageActionService;
import nl.dennisschroer.messagestub.representation.action.MessageActionResultRepresentationMapper;
import nl.dennisschroer.messagestub.representation.message.MessageRepresentation;
import nl.dennisschroer.messagestub.representation.message.MessageRepresentationMapper;
import nl.dennisschroer.messagestub.representation.message.MessagesRepresentation;
import nl.dennisschroer.messagestub.representation.action.MessageActionResultRepresentation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityNotFoundException;

@Controller
@CommonsLog
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    private final MessageActionService messageActionService;

    private final MessageRepresentationMapper messageRepresentationMapper;

    private final MessageActionResultRepresentationMapper messageActionResultRepresentationMapper;

    public MessageController(MessageService messageService, MessageActionService messageActionService, MessageRepresentationMapper messageRepresentationMapper, MessageActionResultRepresentationMapper messageActionResultRepresentationMapper) {
        this.messageService = messageService;
        this.messageActionService = messageActionService;
        this.messageRepresentationMapper = messageRepresentationMapper;
        this.messageActionResultRepresentationMapper = messageActionResultRepresentationMapper;
    }

    @ResponseBody
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public MessagesRepresentation getMessages() {
        return messageRepresentationMapper.toRepresentation(messageService.getMessages());
    }

    @ResponseBody
    @GetMapping(value = "/{id}/", produces = MediaType.APPLICATION_JSON_VALUE)
    public MessageRepresentation getMessage(@PathVariable("id") Long id) {
        return messageRepresentationMapper.toRepresentation(getMessageOrThrowException(id));
    }

    @ResponseBody
    @GetMapping(value = "/{id}/action/{actionName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MessageActionResultRepresentation executeAction(@PathVariable("id") Long id, @PathVariable("actionName") String actionName) {
        Message message = getMessageOrThrowException(id);
        MessageAction messageAction = getMessageActionOrThrowException(actionName, message);

        try {
            log.info(String.format("Executing action %s (%s) on message %d of type %s",
                    messageAction.getName(), messageAction.getClass().getSimpleName(), message.getId(), message.getType()));

            return messageActionResultRepresentationMapper.toRepresentation(messageAction.execute(message));
        } catch (Exception e) {
            log.error("Error while executing action " + messageAction.getName(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Message getMessageOrThrowException(Long id) {
        return messageService.getMessage(id).orElseThrow(() -> new EntityNotFoundException(String.format("Message with id %d not found", id)));
    }

    private MessageAction getMessageActionOrThrowException(String actionName, Message message) {
        return messageActionService.getAction(actionName, message.getType()).orElseThrow(() ->
                new EntityNotFoundException(String.format("No MessageAction with name %s found which is applicable to type %s", actionName, message.getType())));
    }
}
