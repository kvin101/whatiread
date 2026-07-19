export type ReadingStatus = 'TO_READ' | 'READING' | 'READ' | 'DNF'
export type ShelfVisibility = 'SECRET' | 'PRIVATE' | 'FRIENDS' | 'PUBLIC'
export type CommentTargetType = 'SHELF' | 'USER_BOOK' | 'BOOK'
export type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
export type RecommendationStatus = 'PENDING' | 'ACCEPTED' | 'DISMISSED' | 'WITHDRAWN'
export type RecommendationSource = 'FRIEND' | 'SYSTEM'
export type RecommendationTargetType = 'BOOK' | 'SHELF'

export interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  type?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first?: boolean
  last?: boolean
}

export interface CursorPage<T> {
  items: T[]
  nextCursor: string | null
  hasMore: boolean
}

export interface User {
  id: string
  email: string
  username: string
  firstName?: string
  lastName?: string
  displayName?: string
  phoneNumber?: string
  avatarUrl?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  state?: string
  postalCode?: string
  country?: string
  writer?: boolean
  writerBio?: string
  acceptRecommendations?: boolean
  createdAt?: string
  admin?: boolean
}

export type AdminUserRole = 'USER' | 'ADMIN'

export interface AdminUser {
  id: string
  email: string
  username: string
  firstName?: string
  lastName?: string
  displayName?: string
  admin: boolean
  enabled: boolean
  createdAt?: string
}

export interface AdminCreateUserRequest {
  email: string
  username: string
  password: string
  firstName: string
  lastName?: string
  role: AdminUserRole
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: User
}

export interface UsernameAvailability {
  username: string
  valid: boolean
  available: boolean
  message?: string | null
}

export interface Book {
  id: string
  title: string
  subtitle?: string
  authors: string[]
  isbn?: string
  pageCount?: number
  publishYear?: number
  coverUrl?: string
  description?: string
  averageRating?: number
  ratingCount?: number
}

export interface BookSearchResult {
  id?: string
  title: string
  subtitle?: string
  authors: string[]
  isbn?: string
  pageCount?: number
  publishYear?: number
  coverUrl?: string
  externalId?: string
  source?: 'MANUAL' | 'OPEN_LIBRARY' | 'GOODREADS'
}

export interface BookPreview {
  title: string
  subtitle?: string
  authors: string[]
  isbn?: string
  pageCount?: number
  publishYear?: number
  coverUrl?: string
  description?: string
  subjects?: string[]
  averageRating?: number
  ratingCount?: number
  source?: BookSearchResult['source']
  externalId?: string
}

export interface BookSuggestResult {
  title: string
}

export interface UserSuggestResult {
  id: string
  username: string
  displayName: string
}

export interface AdminUserSuggestResult {
  id: string
  username: string
  displayName: string
  email: string
}

export type LibrarySortParam = 'UPDATED_DESC' | 'TITLE_ASC' | 'FINISHED_DESC'

export interface ReadingGoal {
  year: number
  targetBooks?: number | null
  targetPages?: number | null
}

export interface ReadingStats {
  year: number
  booksRead: number
  pagesRead: number
  targetBooks?: number | null
  targetPages?: number | null
  booksProgressPercent?: number | null
  pagesProgressPercent?: number | null
}

export interface ReadingStreak {
  currentStreak: number
  longestStreak: number
  lastActivityDate?: string | null
}

export interface ActivityItem {
  id: string
  eventType: ShelfEventType
  actorId: string
  actorDisplayName: string
  shelfId: string
  shelfName: string
  shelfOwnerId: string
  shelfOwnerDisplayName: string
  payload?: Record<string, string>
  createdAt: string
}

export type NotificationType =
  | 'FRIEND_REQUEST'
  | 'RECOMMENDATION'
  | 'MESSAGE'
  | 'MENTION'
  | 'SHELF_INVITE'
  | 'COMMENT_REPLY'

export interface AppNotification {
  id: string
  type: NotificationType
  payload?: Record<string, string>
  readAt?: string | null
  createdAt: string
}

export interface Author {
  id: string
  slug: string
  name: string
  bio?: string | null
  photoUrl?: string | null
  openLibraryAuthorId?: string | null
}

export interface UserBookNote {
  id: string
  body: string
  authorId: string
  createdAt: string
  updatedAt: string
}

export interface UserBook {
  id: string
  book: Book
  status: ReadingStatus
  rating?: number
  progressPages?: number
  pageCount?: number
  progressPercent?: number
  progressDisplay?: string
  startedAt?: string
  finishedAt?: string
  notes?: UserBookNote[]
  createdAt: string
  updatedAt: string
}

export interface UserProfile {
  id: string
  username: string
  displayName?: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
  writer?: boolean
  writerBio?: string
  friend: boolean
  self: boolean
  blocked: boolean
  blockedByViewer: boolean
}

export type ExploreShelfSource = 'PUBLIC' | 'FRIEND' | 'SHARED'

export interface ExploreShelf {
  id: string
  name: string
  slug: string
  description?: string
  icon?: string
  visibility: ShelfVisibility
  source: ExploreShelfSource
  bookCount: number
  ownerId: string
  ownerDisplayName: string
  updatedAt: string
}

export interface ShelfShareLink {
  id: string
  token: string
  shelfId: string
  createdAt: string
  expiresAt?: string
  revokedAt?: string
  active: boolean
}

export interface SharedShelf {
  shelf: Shelf
  books: ShelfBook[]
}

export interface Shelf {
  id: string
  name: string
  slug: string
  description?: string
  icon?: string
  visibility: ShelfVisibility
  requiresPin: boolean
  sortOrder: number
  ownerId: string
  ownerDisplayName?: string
  currentUserRole?: 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER'
  bookCount: number
  createdAt: string
  updatedAt: string
  clonedFromShelfId?: string | null
  clonedFromShelfName?: string | null
  clonedFromOwnerDisplayName?: string | null
}

export interface ShelfReadingOverlap {
  bookId: string
  bookTitle: string
  readers: { userId: string; displayName: string }[]
}

export interface ShelfMember {
  id: string
  userId: string
  displayName?: string
  role: 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER'
  invitedBy?: string
  createdAt: string
}

export type ShelfEventType =
  | 'SHELF_CREATED'
  | 'SHELF_UPDATED'
  | 'VISIBILITY_CHANGED'
  | 'BOOK_ADDED'
  | 'BOOK_REMOVED'
  | 'MEMBER_ADDED'
  | 'MEMBER_REMOVED'
  | 'MEMBER_ROLE_CHANGED'

export interface ShelfEvent {
  id: string
  eventType: ShelfEventType
  actorId: string
  actorDisplayName: string
  payload: Record<string, string>
  createdAt: string
}

export type MentionType = 'USER' | 'BOOK' | 'SHELF'

export interface MessageMention {
  type: MentionType
  targetId: string
  label: string
}

export interface SystemShelf {
  status: ReadingStatus
  label: string
}

export interface ShelfBook {
  userBookId: string
  userBook: UserBook
  position: number
  visibility?: ShelfVisibility
  effectiveVisibility: ShelfVisibility
  addedBy: string
  createdAt: string
  updatedAt: string
}

export interface FriendSummary {
  id: string
  email: string
  firstName?: string
  lastName?: string
  displayName?: string
  avatarUrl?: string
  friendsSince: string
}

export interface BlockedUser {
  id: string
  firstName?: string
  lastName?: string
  displayName?: string
  avatarUrl?: string
  blockedAt: string
}

export interface FriendUser {
  id: string
  email: string
  firstName?: string
  lastName?: string
  displayName?: string
  avatarUrl?: string
}

export interface FriendRequest {
  id: string
  requester: FriendUser
  addressee: FriendUser
  status: FriendRequestStatus
  createdAt: string
  updatedAt: string
}

export interface CommentAuthor {
  id: string
  displayName?: string
  avatarUrl?: string
}

export interface Comment {
  id: string
  targetType: CommentTargetType
  targetId: string
  author: CommentAuthor
  body: string
  createdAt: string
  updatedAt: string
}

export interface RecommendationUser {
  id: string
  displayName?: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
}

export interface Recommendation {
  id: string
  fromUser: RecommendationUser
  toUser: RecommendationUser
  targetType: RecommendationTargetType
  book?: Book
  shelf?: Shelf
  message?: string
  source: RecommendationSource
  status: RecommendationStatus
  createdAt: string
}

export interface RecommendationSuggestion {
  book: Book
  source: RecommendationSource
  reason: string
}

export type ConversationType = 'DIRECT' | 'GROUP'

export interface ConversationParticipant {
  id: string
  displayName?: string
  firstName?: string
  lastName?: string
  avatarUrl?: string
}

export interface ChatTypingEvent {
  conversationId: string
  userId: string
  typing: boolean
}

export interface Message {
  id: string
  conversationId: string
  senderId: string
  body: string
  mentions?: MessageMention[]
  sentAt: string
  readAt?: string
}

export interface Conversation {
  id: string
  type: ConversationType
  name?: string
  otherParticipant?: ConversationParticipant
  participants?: ConversationParticipant[]
  memberCount: number
  createdById?: string
  viewerIsAdmin: boolean
  viewerIsCreator: boolean
  lastMessage?: Message
  createdAt: string
  unreadCount: number
}
