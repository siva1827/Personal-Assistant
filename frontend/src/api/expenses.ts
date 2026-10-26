import { api } from './client'
import type { Budget, Category, Expense, ExpenseRequest, MonthSummary, Page } from './types'

export const listCategories = () => api.get<Category[]>('/categories').then((r) => r.data)

export const createCategory = (name: string, emoji?: string) =>
  api.post<Category>('/categories', { name, emoji }).then((r) => r.data)

export const listExpenses = (month: string) =>
  api.get<Page<Expense>>('/expenses', { params: { month, size: 100 } }).then((r) => r.data)

export const getSummary = (month: string) =>
  api.get<MonthSummary>('/expenses/summary', { params: { month } }).then((r) => r.data)

export const createExpense = (request: ExpenseRequest) =>
  api.post<Expense>('/expenses', request).then((r) => r.data)

export const updateExpense = (id: number, request: Partial<ExpenseRequest>) =>
  api.patch<Expense>(`/expenses/${id}`, request).then((r) => r.data)

export const deleteExpense = (id: number) => api.delete(`/expenses/${id}`)

export const listBudgets = (month: string) =>
  api.get<Budget[]>('/budgets', { params: { month } }).then((r) => r.data)

export const setBudget = (categoryId: number, month: string, limitAmount: number) =>
  api.put<Budget>(`/budgets/${categoryId}`, { limitAmount }, { params: { month } }).then((r) => r.data)

export const clearBudget = (categoryId: number, month: string) =>
  api.delete(`/budgets/${categoryId}`, { params: { month } })
