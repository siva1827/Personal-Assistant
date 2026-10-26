import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  clearBudget,
  createExpense,
  deleteExpense,
  getSummary,
  listBudgets,
  listCategories,
  listExpenses,
  setBudget,
  updateExpense,
} from '../../api/expenses'
import type { ExpenseRequest } from '../../api/types'

export const useCategories = () => useQuery({ queryKey: ['categories'], queryFn: listCategories })

export const useExpenseList = (month: string) =>
  useQuery({ queryKey: ['expenses', 'list', month], queryFn: () => listExpenses(month) })

export const useSummary = (month: string) =>
  useQuery({ queryKey: ['expenses', 'summary', month], queryFn: () => getSummary(month) })

export const useBudgets = (month: string) =>
  useQuery({ queryKey: ['budgets', month], queryFn: () => listBudgets(month) })

/** Any change to expenses or budgets affects lists, summaries and budget status. */
function useInvalidateMoney() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['expenses'] }),
      queryClient.invalidateQueries({ queryKey: ['budgets'] }),
    ])
}

export const useCreateExpense = () => {
  const invalidate = useInvalidateMoney()
  return useMutation({ mutationFn: (request: ExpenseRequest) => createExpense(request), onSuccess: invalidate })
}

export const useUpdateExpense = () => {
  const invalidate = useInvalidateMoney()
  return useMutation({
    mutationFn: ({ id, ...request }: { id: number } & Partial<ExpenseRequest>) => updateExpense(id, request),
    onSuccess: invalidate,
  })
}

export const useDeleteExpense = () => {
  const invalidate = useInvalidateMoney()
  return useMutation({ mutationFn: deleteExpense, onSuccess: invalidate })
}

export const useSetBudget = (month: string) => {
  const invalidate = useInvalidateMoney()
  return useMutation({
    mutationFn: ({ categoryId, limitAmount }: { categoryId: number; limitAmount: number }) =>
      setBudget(categoryId, month, limitAmount),
    onSuccess: invalidate,
  })
}

export const useClearBudget = (month: string) => {
  const invalidate = useInvalidateMoney()
  return useMutation({ mutationFn: (categoryId: number) => clearBudget(categoryId, month), onSuccess: invalidate })
}
