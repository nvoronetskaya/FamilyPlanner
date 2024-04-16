# Welcome to Cloud Functions for Firebase for Python!
# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from firebase_functions.firestore_fn import (
    on_document_created,
    on_document_updated,
    Event,
    DocumentSnapshot,
    Change
)
from firebase_admin import initialize_app, firestore, messaging
import google.cloud.firestore

app = initialize_app()
firestore_client: google.cloud.firestore.Client = firestore.client()


@on_document_created(document="eventAttendees/{Id}")
def notify_invited_users(attendee: Event[DocumentSnapshot]) -> None:
    if attendee.data is None:
        return

    attendee_info = attendee.data.to_dict()
    event = firestore_client.collection("events").document(attendee_info['eventId']).get().to_dict()
    user = firestore_client.collection("users").document(attendee_info['userId']).get().to_dict()
    data = {
        'type': 'EVENT',
        'sourceId': event['id'],
        'title': 'Новое мероприятие',
        'body': f"Вы приглашены на мероприятие {event['name']}"
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)


@on_document_created(document="observers/{Id}")
def notify_invited_users(observer: Event[DocumentSnapshot]) -> None:
    if observer.data is None:
        return

    observer_info = observer.data.to_dict()
    task = firestore_client.collection("tasks").document(observer_info['taskId']).get().to_dict()
    user = firestore_client.collection("users").document(observer_info['userId']).get().to_dict()
    if user['isExecutor']:
        body = f"Вы назначены исполнителем задачи {task['name']}"
    else:
        body = f"Доступна задача {task['name']}"
    data = {
        'type': 'TASK',
        'sourceId': task['id'],
        'title': 'Новая задача',
        'body': body
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)


@on_document_updated(document="observers/{Id}")
def notify_invited_users(observer: Event[Change[DocumentSnapshot]]) -> None:
    if observer.data is None:
        return
    old_value = observer.data.before.to_dict()
    new_value = observer.data.after.to_dict()
    if old_value['isExecutor'] == new_value['isExecutor'] or not new_value['isExecutor']:
        return
    task = firestore_client.collection("tasks").document(new_value['taskId']).get().to_dict()
    user = firestore_client.collection("users").document(new_value['userId']).get().to_dict()
    data = {
        'type': 'TASK',
        'sourceId': task['id'],
        'title': 'Новая задача',
        'body': f'Вы назначены исполнителем задачи {task['name']}'
    }
    message = messaging.Message(token=user['fcmToken'], data=data)
    messaging.send(message)
