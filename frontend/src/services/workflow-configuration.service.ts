import { api } from "./api";

/**
 * Mirrors the backend `WorkflowConfigurationResponse` record.
 *
 * The same shape is used for reading and writing, which is why this one interface
 * serves both. The backend does the same thing on purpose: the settings screen renders
 * this object into a form and posts the form straight back, so a separate request
 * record would only mean the field list is declared twice and the two copies can drift.
 */
export interface WorkflowConfiguration {
  /**
   * Whether a purchase requisition must be approved by a person before it can be
   * converted into a purchase order.
   *
   * Switching this off does not remove the approval steps already recorded, and
   * switching it on does not retro-approve anything - it only decides what the *next*
   * requisition has to clear.
   */
  purchaseRequisitionApprovalRequired: boolean;
}

const BASE_URL = "/procurement/workflow-configuration";

/**
 * Company workflow settings.
 *
 * One document, so the tag is a plain string rather than an id-bearing tag: there is
 * exactly one configuration per company, and the request is scoped to whichever company
 * the caller's token names.
 */
export const workflowConfigurationApi = api.injectEndpoints({
  endpoints: (build) => ({
    getWorkflowConfiguration: build.query<WorkflowConfiguration, void>({
      query: () => ({
        url: BASE_URL,
        method: "GET",
      }),
      providesTags: ["WorkflowConfiguration"],
    }),

    /**
     * PUT rather than PATCH: the screen edits the whole form and saves it whole, so one
     * replacement with one meaning is the honest contract.
     */
    updateWorkflowConfiguration: build.mutation<
      WorkflowConfiguration,
      WorkflowConfiguration
    >({
      query: (body) => ({
        url: BASE_URL,
        method: "PUT",
        data: body,
      }),
      invalidatesTags: ["WorkflowConfiguration", "PurchaseRequisition"],
    }),
  }),
});

export const {
  useGetWorkflowConfigurationQuery,
  useUpdateWorkflowConfigurationMutation,
} = workflowConfigurationApi;
