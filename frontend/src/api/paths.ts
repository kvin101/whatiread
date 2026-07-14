/** REST API v1 path prefix and builders. */
export const API_V1 = '/api/v1' as const

export function apiV1(path: string): string {
  return `${API_V1}${path}`
}

export const API_PATHS = {
  auth: {
    register: apiV1('/auth/register'),
    login: apiV1('/auth/login'),
    logout: apiV1('/auth/logout'),
    refresh: apiV1('/auth/refresh'),
    usernameAvailable: apiV1('/auth/username/available'),
  },
  me: apiV1('/me'),
  meAvatar: apiV1('/me/avatar'),
  meUsernameAvailable: apiV1('/me/username/available'),
  setup: {
    required: apiV1('/setup/required'),
    admin: apiV1('/setup/admin'),
  },
  books: {
    root: apiV1('/books'),
    search: apiV1('/books/search'),
    suggest: apiV1('/books/suggest'),
    externalPreview: apiV1('/books/external-preview'),
    byId: (bookId: string) => apiV1(`/books/${bookId}`),
  },
  library: {
    root: apiV1('/library'),
    byId: (userBookId: string) => apiV1(`/library/${userBookId}`),
    byBook: (bookId: string) => apiV1(`/library/by-book/${bookId}`),
    notes: (userBookId: string) => apiV1(`/library/${userBookId}/notes`),
    note: (userBookId: string, noteId: string) => apiV1(`/library/${userBookId}/notes/${noteId}`),
  },
  shelves: {
    root: apiV1('/shelves'),
    system: apiV1('/shelves/system'),
    systemBooks: (status: string) => apiV1(`/shelves/system/${status}/books`),
    explore: apiV1('/shelves/explore'),
    byId: (shelfId: string) => apiV1(`/shelves/${shelfId}`),
    members: (shelfId: string) => apiV1(`/shelves/${shelfId}/members`),
    member: (shelfId: string, memberUserId: string) => apiV1(`/shelves/${shelfId}/members/${memberUserId}`),
    events: (shelfId: string) => apiV1(`/shelves/${shelfId}/events`),
    clone: (shelfId: string) => apiV1(`/shelves/${shelfId}/clone`),
    shareClone: (token: string) => apiV1(`/shelves/share/${token}/clone`),
    shareLinks: (shelfId: string) => apiV1(`/shelves/${shelfId}/share-links`),
    shareLink: (shelfId: string, linkId: string) => apiV1(`/shelves/${shelfId}/share-links/${linkId}`),
    books: (shelfId: string) => apiV1(`/shelves/${shelfId}/books`),
    book: (shelfId: string, userBookId: string) => apiV1(`/shelves/${shelfId}/books/${userBookId}`),
  },
  friends: {
    root: apiV1('/friends'),
    requests: apiV1('/friends/requests'),
    requestsIncoming: apiV1('/friends/requests/incoming'),
    requestsOutgoing: apiV1('/friends/requests/outgoing'),
    request: (requestId: string) => apiV1(`/friends/requests/${requestId}`),
    requestAccept: (requestId: string) => apiV1(`/friends/requests/${requestId}/accept`),
    requestDecline: (requestId: string) => apiV1(`/friends/requests/${requestId}/decline`),
    byId: (friendUserId: string) => apiV1(`/friends/${friendUserId}`),
    block: (userId: string) => apiV1(`/friends/${userId}/block`),
    blocked: apiV1('/friends/blocked'),
  },
  comments: {
    root: apiV1('/comments'),
    byId: (commentId: string) => apiV1(`/comments/${commentId}`),
    report: (commentId: string) => apiV1(`/comments/${commentId}/report`),
  },
  users: {
    profile: (userId: string) => apiV1(`/users/${userId}/profile`),
    shelves: (userId: string) => apiV1(`/users/${userId}/shelves`),
    suggest: apiV1('/users/suggest'),
  },
  conversations: {
    root: apiV1('/conversations'),
    groups: apiV1('/conversations/groups'),
    unreadCount: apiV1('/conversations/unread-count'),
    withFriend: (friendUserId: string) => apiV1(`/conversations/with/${friendUserId}`),
    byId: (conversationId: string) => apiV1(`/conversations/${conversationId}`),
    members: (conversationId: string) => apiV1(`/conversations/${conversationId}/members`),
    member: (conversationId: string, memberUserId: string) =>
      apiV1(`/conversations/${conversationId}/members/${memberUserId}`),
    leave: (conversationId: string) => apiV1(`/conversations/${conversationId}/leave`),
    messages: (conversationId: string) => apiV1(`/conversations/${conversationId}/messages`),
    markRead: (conversationId: string) => apiV1(`/conversations/${conversationId}/read`),
  },
  recommendations: {
    root: apiV1('/recommendations'),
    batch: apiV1('/recommendations/batch'),
    inbox: apiV1('/recommendations/inbox'),
    sent: apiV1('/recommendations/sent'),
    suggestions: apiV1('/recommendations/suggestions'),
    accept: (recommendationId: string) => apiV1(`/recommendations/${recommendationId}/accept`),
    dismiss: (recommendationId: string) => apiV1(`/recommendations/${recommendationId}/dismiss`),
    delete: (recommendationId: string) => apiV1(`/recommendations/${recommendationId}`),
  },
  admin: {
    users: apiV1('/admin/users'),
    usersSuggest: apiV1('/admin/users/suggest'),
    user: (userId: string) => apiV1(`/admin/users/${userId}`),
    userPassword: (userId: string) => apiV1(`/admin/users/${userId}/password`),
    userEnabled: (userId: string) => apiV1(`/admin/users/${userId}/enabled`),
    registration: apiV1('/admin/instance/registration'),
  },
} as const

export const WS_PATH = '/ws' as const

export const WS_STOMP = {
  sendMessage: '/app/chat.send',
  sendTyping: '/app/chat.typing',
  queueMessages: '/user/queue/messages',
  queueTyping: '/user/queue/typing',
  queueRecommendations: '/user/queue/recommendations',
  queueFriends: '/user/queue/friends',
} as const

export const AUTH_HEADERS = {
  bearer: (token: string) => `Bearer ${token}`,
} as const

/** Client-side route paths (React Router). */
export const APP_ROUTES = {
  setup: '/setup',
  login: '/login',
  register: '/register',
  library: '/library',
  shelves: '/shelves',
  shelf: (shelfId: string) => `/shelves/${shelfId}`,
  explore: '/explore',
  userProfile: (userId: string) => `/users/${userId}`,
  friends: '/friends',
  messages: '/messages',
  recommendations: '/recommendations',
  settings: '/settings',
  adminUsers: '/admin/users',
  sharedShelf: (token: string) => `/share/shelf/${token}`,
} as const

export const PUBLIC_API_PATHS = {
  sharedShelf: (token: string) => apiV1(`/public/shelves/share/${token}`),
} as const
