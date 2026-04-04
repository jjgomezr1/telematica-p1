#ifndef OPERATOR_LIST_H
#define OPERATOR_LIST_H

#define MAX_OPERATORS 50

void operator_list_init(void);
void operator_list_add(int fd);
void operator_list_remove(int fd);
void operator_list_broadcast(const char *mensaje);

#endif // OPERATOR_LIST_H
