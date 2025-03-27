## **Overview**

> **This Ticketing System** is designed to facilitate an organized approach to issue management within any organization.

- **Clients** can create and monitor tickets for any issues they face.
- **Support Agents** are assigned tickets based on workload and priority, ensuring efficient resolution.
- **Admins** have oversight capabilities, including reporting, analytics, and the ability to manage users and roles.

### **Example Workflow**

1. **Client Action:** A client encounters an issue and **creates a ticket**.
2. **System Action:** The system **automatically assigns** the ticket to the available agent with the lowest workload, considering the ticket's priority.
3. **Agent Action:** The assigned agent **updates the ticket's status** and resolution progress.
4. **Admin Action:** An admin **monitors resolution times**, agent performance, and overall ticket statistics.

---

## **Key Features**

### **1. User Management**
- **Registration & Login:**  
  Secure authentication and authorization using **Spring Security** & **JWT**.
- **Role Management:**  
  Supports **Admin**, **Support Agent**, and **Client** roles with distinct permission sets.
- **Permissions:**  
  Fine-grained access control to ensure that each role can only access the features they require.

### **2. Ticket Management**
- **Ticket Creation:**  
  Clients report issues by providing relevant details such as **title**, **description**, and **priority**.
- **Workload-Based Assignment:**  
  Tickets are automatically assigned to agents with the **lowest workload**. Ticket **priority** directly influences workload calculations.
- **Ticket Status Lifecycle:**  
  Each ticket is tracked through its life cycle:  
  > **Open** → **In Progress** → **Resolved** → **Closed**

### **3. Reporting & Analytics (Admin)**
- **Ticket Metrics:**  
  View crucial metrics like **average resolution times**, **ticket volume**, and **agent performance**.
- **Ticket Status Tracking:**  
  Monitor the number of tickets in each status to get a clear picture of operational efficiency.
- **Report Generation (Optional):**  
  Admins have the option to extract **PDF reports** for detailed analysis.

### **4. Communication Module**
- **Comments & Notes:**  
  Both agents and clients can add **comments** and **notes** within tickets to discuss and resolve issues.
- **Email Notifications:**  
  Automated email alerts notify relevant parties whenever there are **updates** on a ticket.
- **Real-time Chat (Optional):**  
  Future enhancements include integrating **live chat support** for immediate assistance.

## Chat Functionality

The application includes a real-time chat system that allows users to communicate about tickets. The chat system uses:

- WebSockets with STOMP protocol for real-time messaging
- SockJS for fallback in environments where WebSockets aren't supported
- JWT authentication to secure WebSocket connections

### Using the Chat

1. Click on the "Chat" button on any ticket to open the chat modal
2. Messages are sent and received in real-time
3. Chat history is loaded automatically when you open the chat

### Developer Information

The chat system consists of:

**Backend**:
- WebSocketConfig: Configures STOMP endpoints and message broker
- ChatController: Handles WebSocket messages and REST endpoints for chat history
- ChatService: Business logic for chat functionality
- WebSocketEventListener: Manages WebSocket connection events
- JwtHandshakeInterceptor: Secures WebSocket connections

**Frontend**:
- chatbox.tsx: UI component for chat modal
- chatService.ts: Service for WebSocket communication

### WebSocket Endpoints

- `/ws-chat`: Main WebSocket endpoint
- `/ws-chat-sockjs`: SockJS fallback endpoint
- `/app/chat`: Destination for public messages
- `/app/private-chat`: Destination for private messages
- `/app/load-history`: Request chat history
