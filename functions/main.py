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
    user = firestore_client.collection("users").document(attendee_info['userId']).get()
    notification = messaging.Notification(
        title="Новое мероприятие",
        body=f"Вы приглашены на мероприятие {event['name']}"
    )

    message = messaging.Message(token=user['fcmToken'], notification=notification)
    messaging.send(message)

@on_document_updated(document="eventAttendees/{Id}")
def notify_invited_users(attendee: Event[Change[DocumentSnapshot]]) -> None:
    if attendee.data is None:
        return

    attendee_info = attendee.data.to_dict()
    event = firestore_client.collection("events").document(attendee_info['eventId']).get().to_dict()
    user = firestore_client.collection("users").document(attendee_info['userId']).get()
    notification = messaging.Notification(
        title="Новое мероприятие",
        body=f"Вы приглашены на мероприятие {event['name']}"
    )

    message = messaging.Message(token=user['fcmToken'], notification=notification)
    messaging.send(message)