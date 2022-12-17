- (SUBJECT TO CHANGE) Firebase will hold data as:
LIST COLLECTION (named lists)
	listId: listID
	listOwner: creatorID
	text: text
	title: title

USER COLLECTION (named users)
	email: user email
	fcmToken: if the user is logged in, the token will be attributed
	image: user's profile image
	myLists: [LIST_REFERENCE1, LIST_REFERENCE2,...]
	name: user's name
	passowrd: currently non encrypted, user password

=========================================================================================
Both user ID's and list ID's are the document's name:
A Collection contains numerous documents which hold the data
=========================================================================================
The Util section of our code has:
Converters (ShoppingList to JSON String and vice versa, and ArrayList to JSON String and vice versa)
Constants (hold all the key values for both collections and elements of the database, is also used
to store key value pairs in SharedMemory(PreferenceManager))
Encoders (has encodeImage, usefull to encode an image)
PreferenceManager (much like flutter, it's a sharedMemory)
=========================================================================================
The QR folder contains both the scanner fragment and the Dialog Fragment, the dialog generates a QR
code based on the list you have open, the QR scanner opens up the camera to scan a QR code
The way QR codes are generated is a JSON String with a List
The way QR codes are scanned is the list is read, the owner's profile picture is fetched from the DB,
a new list_reference is added to myLists in the DB and a new list is created on the viewModel.
=========================================================================================
- By using firestore we can apply listeners to the changes on the collection and update when anything happens
e.g. ListenToListChanges() in NoteFragment and in ListFragment (same name, different handling methods)
