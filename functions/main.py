# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from firebase_functions.firestore_fn import (
    on_document_created,
    on_document_updated,
    on_document_deleted,
    Event,
    DocumentSnapshot,
    Change
)
from firebase_admin import initialize_app, firestore, messaging
import google.cloud.firestore

app = initialize_app()
firestore_client: google.cloud.firestore.Client = firestore.client()


@on_document_created(document="eventAttendee/{Id}")
def notify_invited_users(attendee: Event[DocumentSnapshot]) -> None:
    if attendee.data is None:
        return
    attendee_info = attendee.data.to_dict()
    event = firestore_client.document(f"event/{attendee_info['eventId']}").get().to_dict()
    user = firestore_client.document(f"user/{attendee_info['userId']}").get().to_dict()
    if event['createdBy'] == attendee_info['userId']:
        return
    data = {
        'type': 'EVENT',
        'sourceId': attendee_info['eventId'],
        'title': 'Новое мероприятие',
        'body': f"Вы приглашены на мероприятие {event['name']}"
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)


@on_document_created(document="observer/{Id}")
def notify_task_observers(observer: Event[DocumentSnapshot]) -> None:
    if observer.data is None:
        return

    observer_info = observer.data.to_dict()
    task = firestore_client.document(f"task/{observer_info['taskId']}").get().to_dict()
    user = firestore_client.document(f"user/{observer_info['userId']}").get().to_dict()
    if task['createdBy'] == observer_info['userId']:
        return
    if observer_info.get('isExecutor'):
        body = f"Вы назначены исполнителем задачи {task['name']}"
    else:
        body = f"Доступна задача {task['title']}"
    data = {
        'type': 'TASK',
        'sourceId': observer_info['taskId'],
        'title': 'Новая задача',
        'body': body
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)


@on_document_updated(document="observer/{Id}")
def notify_new_executors(observer: Event[Change[DocumentSnapshot]]) -> None:
    if observer.data is None:
        return
    old_value = observer.data.before.to_dict()
    new_value = observer.data.after.to_dict()
    if old_value['isExecutor'] == new_value['isExecutor'] or not new_value['isExecutor']:
        return
    task = firestore_client.document(f"task/{new_value['taskId']}").get().to_dict()
    user = firestore_client.document(f"user/{new_value['userId']}").get().to_dict()
    data = {
        'type': 'TASK',
        'sourceId': new_value['taskId'],
        'title': 'Новая задача',
        'body': f'Вы назначены исполнителем задачи {task['title']}'
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)


@on_document_updated(document="task/{Id}")
def notify_executors_on_task_change(task: Event[Change[DocumentSnapshot]]) -> None:
    if task.data is None:
        return
    old_value = task.data.before.to_dict()
    new_value = task.data.after.to_dict()
    observers = firestore_client.collection('observer').where('taskId', '==', task.data.after.id).get()
    for observer in observers:
        user = firestore_client.document(f"user/{observer.to_dict()['userId']}").get().to_dict()
        data = {
            'type': 'TASK',
            'sourceId': task.id,
            'title': 'Задача изменена',
            'body': f'Задача {old_value['title']} была изменена. Нажмите, чтобы увидеть подробности'
        }
        message = messaging.Message(token=user['fcmToken'], data=data)
        messaging.send(message)


@on_document_deleted(document="event/{Id}")
def notify_attendees_event_cancel(event: Event[DocumentSnapshot | None]) -> None:
    if event.data is None:
        return
    attendees = firestore_client.collection('eventAttendee').where('eventId', '==', event.data.id).get()
    event_data = event.data.to_dict()
    for attendee in attendees:
        attendee_data = attendee.to_dict()
        if 'NOT_COMING' in attendee_data.values():
            continue
        user = firestore_client.document(f"user/{attendee_data['userId']}").get().to_dict()
        data = {
            'type': 'EVENT',
            'title': 'Мероприятие отменено',
            'body': f'Мероприятие {event_data['name']} было отменено'
        }
        message = messaging.Message(token=user['fcmToken'], data=data)
        messaging.send(message)


@on_document_updated(document="event/{Id}")
def notify_attendees_on_event_change(event: Event[Change[DocumentSnapshot]]) -> None:
    if event.data is None:
        return
    old_value = event.data.before.to_dict()
    attendees = firestore_client.collection('eventAttendee').where('eventId', '==', event.data.after.id).get()
    for attendee in attendees:
        user = firestore_client.document(f"user/{attendee.to_dict()['userId']}").get().to_dict()
        data = {
            'type': 'EVENT',
            'sourceId': event.data.after.id,
            'title': 'Мероприятие изменено',
            'body': f'Мероприятие {old_value['name']} было изменено. Нажмите, чтобы увидеть подробности'
        }
        message = messaging.Message(token=user['fcmToken'], data=data)
        messaging.send(message)


@on_document_created(document="userList/{Id}")
def notify_list_observers(userList: Event[DocumentSnapshot]) -> None:
    if userList.data is None:
        return
    userListData = userList.data.to_dict()
    grocery_list = firestore_client.document(f"list/{userListData['listId']}").get().to_dict()
    if userListData['userId'] == grocery_list['createdBy']:
        return
    user = firestore_client.document(f"user/{userListData['userId']}").get().to_dict()
    data = {
        'type': 'LIST',
        'sourceId': userListData['listId'],
        'title': 'Новый список покупок',
        'body': f'Вам доступен список покупок {grocery_list['name']}'
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)
